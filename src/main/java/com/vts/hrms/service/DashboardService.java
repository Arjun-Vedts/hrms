package com.vts.hrms.service;

import com.vts.hrms.dto.*;
import com.vts.hrms.entity.*;
import com.vts.hrms.repository.CourseRepository;
import com.vts.hrms.repository.EvaluationRepository;
import com.vts.hrms.repository.RequisitionRepository;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    private static final Logger log = LoggerFactory.getLogger(DashboardService.class);

    @Value("${x_api_key}")
    private String xApiKey;

    private final CourseRepository courseRepository;
    private final RequisitionRepository requisitionRepository;
    private final MasterCacheService masterCacheService;
    private final MasterClientService masterClientService;
    private final EvaluationRepository evaluationRepository;

    public DashboardService(CourseRepository courseRepository, RequisitionRepository requisitionRepository, MasterCacheService masterCacheService, MasterClientService masterClientService, EvaluationRepository evaluationRepository) {
        this.courseRepository = courseRepository;
        this.requisitionRepository = requisitionRepository;
        this.masterCacheService = masterCacheService;
        this.masterClientService = masterClientService;
        this.evaluationRepository = evaluationRepository;
    }

    public List<CourseDashboardDTO> getOrganizerCourseDashboard(LocalDate startDate, LocalDate endDate) {
        log.info("Fetching organizer wise course dashboard from {} to {}", startDate, endDate);

        return courseRepository.getOrganizerWiseCourseCount(startDate, endDate);
    }

    public RequisitionDashboardDTO getDashboardData(LocalDate startDate, LocalDate endDate) {

        log.info("Fetching requisition dashboard data from {} to {}", startDate, endDate);

        RequisitionDashboardDTO dto = new RequisitionDashboardDTO();


        List<Requisition> requisitions =
                Optional.ofNullable(requisitionRepository.getRequisitionDataByDateRange(startDate, endDate))
                        .orElse(Collections.emptyList());


        if (requisitions.isEmpty()) {
            dto.setOrganisers(0L);
            dto.setCourses(0L);
            dto.setRequisitions(0L);
            dto.setAttended(0L);
            dto.setCourseCounts(Collections.emptyList());
            dto.setAttendedByCadre(null);
            dto.setNotAttendedByCadre(null);
            dto.setCourseParticipants(Collections.emptyList());
            log.info("No requisition data found between {} and {}", startDate, endDate);
            return dto;
        }

        Map<Long, Course> courseMap = Optional.ofNullable(masterCacheService.getCourseMap())
                .orElse(Collections.emptyMap());

        Map<Long, CourseType> courseTypeMap = Optional.ofNullable(masterCacheService.getCourseTypeMap())
                .orElse(Collections.emptyMap());

        List<EmployeeDTO> employeeList = Optional.ofNullable(masterClientService.getEmployeeMasterList(xApiKey))
                .orElse(Collections.emptyList());

        Map<Long, EmployeeDTO> employeeMap =
                employeeList.stream()
                        .filter(Objects::nonNull)
                        .filter(employee -> employee.getEmpId() != null)
                        .collect(
                                Collectors.toMap(EmployeeDTO::getEmpId, Function.identity(),
                                        (first, second) -> first
                                )
                        );


        long requisitionCount = requisitions.size();
        long attendedCount = requisitions.stream().filter(this::isAttended).count();

        long uniqueCourseCount = requisitions.stream()
                .map(Requisition::getCourseId)
                .filter(Objects::nonNull)
                .distinct()
                .count();

        long organiserCount = requisitions.stream()
                .map(Requisition::getCourseId)
                .filter(Objects::nonNull)
                .map(courseMap::get)
                .filter(Objects::nonNull)
                .map(Course::getOrganizerId)
                .filter(Objects::nonNull)
                .distinct()
                .count();


        Map<String, Long> attendedCadreCounts = createCountMap(CADRES);
        Map<String, Long> notAttendedCadreCounts = createCountMap(CADRES);
        Map<String, Long> courseTypeCounts = createCountMap(COURSE_TYPES);

        Map<Long, CourseDashboardAccumulator> courseAccumulator = new LinkedHashMap<>();

        for (Requisition requisition : requisitions) {

            if (requisition == null) {
                continue;
            }

            Long participantId = requisition.getInitiatingOfficer();
            EmployeeDTO employee = participantId == null ? null : employeeMap.get(participantId);
            String cadre = resolveCadre(employee);

            if (isAttended(requisition)) {
                attendedCadreCounts.merge(cadre, 1L, Long::sum);
            } else {
                notAttendedCadreCounts.merge(cadre, 1L, Long::sum);
            }

            Long courseId = requisition.getCourseId();
            if (courseId == null) {
                continue;
            }

            Course course = courseMap.get(courseId);
            if (course == null) {
                log.debug("Course not found in cache for courseId={}", courseId);
                continue;
            }

            // NEW — course type bucket, reusing the course we already fetched
            CourseType courseType = courseTypeMap.get(course.getCourseTypeId());
            String resolvedType = resolveCourseType(courseType);
            courseTypeCounts.merge(resolvedType, 1L, Long::sum);

            CourseDashboardAccumulator accumulator =
                    courseAccumulator.computeIfAbsent(
                            courseId,
                            id -> new CourseDashboardAccumulator(id, resolveCourseName(course))
                    );
            accumulator.incrementTotal();
            accumulator.incrementCadre(cadre);
        }

        dto.setOrganisers(organiserCount);
        dto.setCourses(uniqueCourseCount);
        dto.setRequisitions(requisitionCount);
        dto.setAttended(attendedCount);
        dto.setCourseCounts(toCountTypeResponseList(COURSE_TYPES, courseTypeCounts));
        dto.setAttendedByCadre(toCountTypeResponseList(CADRES, attendedCadreCounts));
        dto.setNotAttendedByCadre(toCountTypeResponseList(CADRES, notAttendedCadreCounts));

        List<CountTypeResponse> courseResponses =
                courseAccumulator.values()
                        .stream()
                        .map(this::toCourseResponse)
                        .toList();

        dto.setCourseParticipants(courseResponses);


        log.info(
                "Requisition dashboard generated successfully. " +
                        "Requisitions={}, Courses={}, Organisers={}, Attended={}, " +
                        "AttendedCadres={}, NotAttendedCadres={}, CoursesData={}",
                requisitionCount,
                uniqueCourseCount,
                organiserCount,
                attendedCount,
                attendedCadreCounts.size(),
                notAttendedCadreCounts.size(),
                courseResponses.size()
        );

        return dto;
    }

    private static final List<String> CADRES = List.of(
            "DRDS", "DRTC", "Admin & Allied", "Service Personnel", "Others"
    );

    private static final List<String> COURSE_TYPES = List.of(
            "Training", "Seminar", "Symposium", "Conference", "Workshop"
    );

    private Map<String, Long> createCountMap(List<String> keys) {
        Map<String, Long> map = new LinkedHashMap<>();
        keys.forEach(key -> map.put(key, 0L));
        return map;
    }

    private List<CountTypeResponse> toCountTypeResponseList(List<String> keys, Map<String, Long> counts) {
        Map<String, Long> source = (counts == null) ? Collections.emptyMap() : counts;
        List<CountTypeResponse> response = new ArrayList<>();
        for (String key : keys) {
            response.add(new CountTypeResponse(key, source.getOrDefault(key, 0L), null, null));
        }
        return response;
    }

    private String resolveCourseType(CourseType courseType) {
        if (courseType == null || courseType.getCourseTypeId() == null) {
            return "Training";
        }

        String name = courseType.getCourseType();
        if (name == null) return "Training";
        name = name.trim();
        if ("Seminar".equalsIgnoreCase(name)) return "Seminar";
        if ("Symposium".equalsIgnoreCase(name)) return "Symposium";
        if ("Conference".equalsIgnoreCase(name)) return "Conference";
        if ("Workshop".equalsIgnoreCase(name)) return "Workshop";
        return "Training";
    }

    private boolean isAttended(Requisition requisition) {
        return requisition != null
                && "Y".equalsIgnoreCase(
                requisition.getIsAttend()
        );
    }

    private String resolveCadre(EmployeeDTO employee) {
        if (employee == null) {
            return "Others";
        }

        String cadre = employee.getDesigCadre();
        if (cadre == null || cadre.isBlank()) {
            return "Others";
        }

        cadre = cadre.trim();
        if ("DRDS".equalsIgnoreCase(cadre)) {
            return "DRDS";
        }

        if ("DRTC".equalsIgnoreCase(cadre)) {
            return "DRTC";
        }

        if ("Service Personnel".equalsIgnoreCase(cadre)) {
            return "Service Personnel";
        }

        if ("Admin & Allied".equalsIgnoreCase(cadre)) {
            return "Admin & Allied";
        }

        return "Others";
    }

    public RequisitionDashboardDTO getUserDashboardData(Long empId, LocalDate startDate, LocalDate endDate) {

        log.info("Fetching requisition user dashboard data for empId {} from {} to {}", empId, startDate, endDate);

        RequisitionDashboardDTO dto = new RequisitionDashboardDTO();

        List<Requisition> requisitions =
                Optional.ofNullable(requisitionRepository.getUserRequisitionDataByDateRange(empId, startDate, endDate))
                        .orElse(Collections.emptyList());

        if (requisitions.isEmpty()) {
            dto.setOrganisers(0L);
            dto.setCourses(0L);
            dto.setRequisitions(0L);
            dto.setAttended(0L);
            log.info("No data found between {} and {}", startDate, endDate);
            return dto;
        }

        Map<Long, Course> courseMap = Optional.ofNullable(masterCacheService.getCourseMap())
                .orElse(Collections.emptyMap());

        long requisitionCount = requisitions.size();
        long attendedCount = requisitions.stream().filter(this::isAttended).count();

        long uniqueCourseCount = requisitions.stream()
                .map(Requisition::getCourseId)
                .filter(Objects::nonNull)
                .distinct()
                .count();

        long organiserCount = requisitions.stream()
                .map(Requisition::getCourseId)
                .filter(Objects::nonNull)
                .map(courseMap::get)
                .filter(Objects::nonNull)
                .map(Course::getOrganizerId)
                .filter(Objects::nonNull)
                .distinct()
                .count();

        dto.setOrganisers(organiserCount);
        dto.setCourses(uniqueCourseCount);
        dto.setRequisitions(requisitionCount);
        dto.setAttended(attendedCount);

        return dto;
    }

    public List<YearlyRequisitionSummary> getUserYearlyTrend(Long empId, int yearsBack) {

        log.info("Fetching {}-year requisition trend for empId {}", yearsBack, empId);

        LocalDate today = LocalDate.now();
        int currentFyStart = today.getMonthValue() >= 4 ? today.getYear() : today.getYear() - 1;
        int earliestFyStart = currentFyStart - (yearsBack - 1);

        LocalDate rangeStart = LocalDate.of(earliestFyStart, 4, 1);
        LocalDate rangeEnd = LocalDate.of(currentFyStart + 1, 3, 31);

        List<Requisition> requisitions =
                Optional.ofNullable(
                        requisitionRepository.getUserRequisitionDataByDateRange(empId, rangeStart, rangeEnd)
                ).orElse(Collections.emptyList());


        Map<String, long[]> buckets = new LinkedHashMap<>(); // [total, attended, notAttended]
        Map<String, LocalDate[]> yearBounds = new LinkedHashMap<>(); // [yearStart, yearEnd]
        List<String> orderedLabels = new ArrayList<>();

        for (int i = 0; i < yearsBack; i++) {
            int startYear = earliestFyStart + i;
            String label = startYear + "-" + String.valueOf(startYear + 1).substring(2);
            orderedLabels.add(label);
            buckets.put(label, new long[3]);
            yearBounds.put(label, new LocalDate[]{
                    LocalDate.of(startYear, 4, 1),
                    LocalDate.of(startYear + 1, 3, 31)
            });
        }

        for (Requisition requisition : requisitions) {

            if (requisition == null
                    || requisition.getFromDate() == null
                    || requisition.getToDate() == null) {
                continue;
            }

            LocalDate fromDate = requisition.getFromDate();
            LocalDate toDate = requisition.getToDate();

            for (String label : orderedLabels) {

                LocalDate[] bounds = yearBounds.get(label);

                boolean withinYear =
                        !fromDate.isBefore(bounds[0]) && !toDate.isAfter(bounds[1]);

                if (withinYear) {
                    long[] bucket = buckets.get(label);
                    bucket[0]++; // total
                    if (isAttended(requisition)) {
                        bucket[1]++;
                    } else {
                        bucket[2]++;
                    }
                    break;
                }
            }
        }

        List<YearlyRequisitionSummary> result = new ArrayList<>();
        for (String label : orderedLabels) {
            long[] c = buckets.get(label);
            result.add(new YearlyRequisitionSummary(label, c[0], c[1], c[2]));
        }
        return result;
    }

    public Map<String, Long> getUserEvaluationData(LocalDate startDate, LocalDate endDate) {
        log.info("Fetching evaluation data for period startDate {} endDate {} ", startDate, endDate);

        List<EvaluationDTO> list = evaluationRepository.findEvaluationData(startDate,endDate);
        return list.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(
                        dto -> dto.getImpact() == null || dto.getImpact().isBlank()
                                ? "Unknown"
                                : dto.getImpact().trim(),
                        Collectors.counting()
                ));
    }

    public List<FeedbackDTO> getUserRequisitionPending(Long empId, LocalDate startDate, LocalDate endDate) {
        log.info("Fetching pending requisition data for empId {} from startDate {} to endDate {}", empId, startDate, endDate);

        return requisitionRepository.findPendingRequisitions(empId, startDate, endDate);
    }


    @Getter
    private static class CourseDashboardAccumulator {

        private final Long courseId;
        private final String courseName;
        private long total;
        private final Map<String, Long> cadreCounts;


        CourseDashboardAccumulator(Long courseId, String courseName) {

            this.courseId = courseId;
            this.courseName = courseName;
            this.cadreCounts = new LinkedHashMap<>();

            this.cadreCounts.put("DRDS", 0L);
            this.cadreCounts.put("DRTC", 0L);
            this.cadreCounts.put("Admin & Allied", 0L);
            this.cadreCounts.put("Service Personnel", 0L);
            this.cadreCounts.put("Others", 0L);
        }


        void incrementTotal() {
            this.total++;
        }

        void incrementCadre(String cadre) {
            this.cadreCounts.merge(cadre, 1L, Long::sum);
        }
    }

    private CountTypeResponse toCourseResponse(CourseDashboardAccumulator accumulator) {

        CountTypeResponse response = new CountTypeResponse();

        response.setType(String.valueOf(accumulator.getCourseId()));
        response.setCount(accumulator.getTotal());
        response.setCourseName(accumulator.getCourseName());
        response.setCadreCounts(new LinkedHashMap<>(accumulator.getCadreCounts()));
        return response;
    }

    private String resolveCourseName(Course course) {
        if (course == null) {
            return "Unknown Course";
        }

        String courseName = course.getCourseName();
        if (courseName == null || courseName.isBlank()) {
            return "Unknown Course";
        }
        return courseName.trim();
    }

}
