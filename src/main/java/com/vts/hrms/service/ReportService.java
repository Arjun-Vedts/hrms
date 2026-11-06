package com.vts.hrms.service;


import com.vts.hrms.dto.*;
import com.vts.hrms.entity.*;
import com.vts.hrms.mapper.*;
import com.vts.hrms.repository.*;
import com.vts.hrms.util.ApiResponse;
import com.vts.hrms.util.CommonUtil;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;


@RequiredArgsConstructor
@Service
public class ReportService {

    private static final Logger log = LoggerFactory.getLogger(ReportService.class);

    @Value("${x_api_key}")
    private String xApiKey;

    @Value("${labCode}")
    private String labCode;

    private final EmsClientService emsClientService;
    private final ObjectMapper objectMapper;
    private final RequisitionMapper requisitionMapper;
    private final RequisitionRepository requisitionRepository;
    private final MasterCacheService masterCacheService;
    private final TrainingService trainingService;
    private final SponsorshipService sponsorshipService;
    private final CourseRepository courseRepository;
    private final CourseTypeRepository courseTypeRepository;
    private final SponsorshipRepository sponsorshipRepository;
    private final CepRepository cepRepository;
    private final CepMapper cepMapper;
    private final SponsorshipMapper sponsorshipMapper;
    private final DistributionRepository distributionRepository;
    private final DistributionMapper distributionMapper;
    private final FeedbackRepository feedbackRepository;


    @Cacheable(value = "getNominalROllList")
    public List<EmployeeDTO> getNominalRollList(String token) {
        log.info("Fetching nominal roll from EMS");

        try {
            ResponseEntity<ApiResponse> responseEntity = emsClientService.getNominalRollList(token);

            if (responseEntity == null || !responseEntity.getStatusCode().is2xxSuccessful()) {
                log.error("Invalid response from EMS");
                throw new RuntimeException("Failed to fetch data from EMS");
            }

            ApiResponse apiResponse = responseEntity.getBody();

            if (apiResponse == null || !apiResponse.isSuccess()) {
                log.error("API returned failure: {}", apiResponse != null ? apiResponse.getMessage() : "null body");
                throw new RuntimeException("EMS returned failure response");
            }

            Object data = apiResponse.getData();
            if (data == null) {
                log.warn("No data found in response");
                return Collections.emptyList();
            }

            // Convert to List<EmployeeDTO>
            return objectMapper.convertValue(data, new TypeReference<List<EmployeeDTO>>() {
            });

        } catch (FeignException.Unauthorized ex) {
            log.error("Unauthorized access", ex);
            throw new RuntimeException("Unauthorized access to EMS");

        } catch (FeignException.ServiceUnavailable ex) {
            log.error("EMS service down", ex);
            throw new RuntimeException("EMS service unavailable");

        } catch (FeignException ex) {
            log.error("Feign error", ex);
            throw new RuntimeException("Error while calling EMS");

        } catch (Exception ex) {
            log.error("Unexpected error", ex);
            throw new RuntimeException("Something went wrong");
        }
    }


    public List<RequisitionDTO> getCourseTrainingList(LocalDate fromDate, LocalDate toDate, String courseType) {

        log.info("Fetching course training data for type: {}", courseType);

        Map<Long, EmployeeDTO> employeeMap = masterCacheService.getLongEmployeeDTOMap();
        Map<Long, Organizer> organizerMap = masterCacheService.getOrganizerMap();
        Map<Long, Course> courseMap = masterCacheService.getCourseMap();
        Map<String, Status> statusMap = masterCacheService.getStatusMap();

        List<CourseTypeDTO> typeDTOList = trainingService.getCourseTypeList("user");

        Map<Long, CourseTypeDTO> typeDTOMap = typeDTOList.stream()
                .collect(Collectors.toMap(
                        CourseTypeDTO::getCourseTypeId,
                        Function.identity()
                ));

        List<Requisition> list = requisitionRepository.getRequisitionDataByDateRange(fromDate, toDate);

        Set<String> seminarTypes = Set.of("Seminar", "Symposium", "Conference", "Workshop");

        boolean isSeminar = "seminar".equalsIgnoreCase(courseType);

        return list.stream()
                .map(requisitionMapper::toDto)
                .filter(dto -> {
                    Course course = courseMap.get(dto.getCourseId());

                    if (course == null) {
                        return false;
                    }

                    CourseTypeDTO typeDTO = typeDTOMap.get(course.getCourseTypeId());

                    if (typeDTO == null ||
                            typeDTO.getCourseType() == null) {
                        return false;
                    }

                    String typeName = typeDTO.getCourseType().trim();

                    boolean isSeminarType = seminarTypes.stream()
                            .anyMatch(type ->
                                    type.equalsIgnoreCase(typeName)
                            );

                    if (isSeminar) {
                        return isSeminarType;
                    }
                    return !isSeminarType;
                })
                .peek(dto -> {

                    Course course = courseMap.get(dto.getCourseId());
                    if (course == null) {
                        return;
                    }

                    Organizer organizer = organizerMap.get(course.getOrganizerId());
                    CourseTypeDTO typeDTO = typeDTOMap.get(course.getCourseTypeId());
                    EmployeeDTO employeeDTO = employeeMap.get(dto.getInitiatingOfficer());
                    Status status = statusMap.get(dto.getStatus());

                    dto.setCourseName(course.getCourseName());
                    dto.setCourseLevel(course.getCourseLevel());

                    if (typeDTO != null) {
                        dto.setCourseType(typeDTO.getCourseType());
                    }

                    dto.setVenue(course.getVenue());

                    if (status != null) {
                        dto.setStatusColor(status.getColorCode());
                        dto.setStatusName(status.getStatusName());
                    }

                    dto.setOfflineRegistrationFee(course.getOfflineRegistrationFee());
                    dto.setOnlineRegistrationFee(course.getOnlineRegistrationFee());

                    if (organizer != null) {
                        dto.setOrganizer(organizer.getOrganizer());
                        dto.setOrganizerContactName(organizer.getContactName());
                        dto.setOrganizerPhoneNo(organizer.getPhoneNo());
                        dto.setOrganizerFaxNo(organizer.getFaxNo());
                        dto.setOrganizerEmail(organizer.getEmail());
                    }

                    if (employeeDTO != null) {
                        dto.setEmpNo(employeeDTO.getEmpNo());
                        dto.setInitiatingOfficerName(CommonUtil.buildEmployeeName(employeeDTO, false));
                        dto.setDesigCadre(employeeDTO.getDesigCadre());
                        dto.setEmpDesigName(employeeDTO.getEmpDesigName());
                        dto.setEmpDivCode(employeeDTO.getEmpDivCode());
                        dto.setEmail(employeeDTO.getEmail());
                        dto.setMobileNo(employeeDTO.getMobileNo());
                    }
                })
                .toList();
    }

    public List<RequisitionDTO> getAllCourseTrainingList() {
        log.info("Fetching all course training data");

        Map<Long, EmployeeDTO> employeeMap = masterCacheService.getLongEmployeeDTOMap();
        Map<Long, Organizer> organizerMap = masterCacheService.getOrganizerMap();
        Map<Long, Course> courseMap = masterCacheService.getCourseMap();
        Map<String, Status> statusMap = masterCacheService.getStatusMap();

        List<CourseTypeDTO> typeDTOList = trainingService.getCourseTypeList("user");
        Map<Long, CourseTypeDTO> typeDTOMap = typeDTOList.stream()
                .collect(Collectors.toMap(CourseTypeDTO::getCourseTypeId, Function.identity()));

        List<Requisition> list = requisitionRepository.findAllByIsAttendAndIsActive("Y", 1);

        return list.stream()
                .map(requisitionMapper::toDto)
                .peek(dto -> {
                    Course course = courseMap.get(dto.getCourseId());
                    Organizer organizer = (course != null) ? organizerMap.get(course.getOrganizerId()) : null;
                    CourseTypeDTO typeDTO = (course != null && course.getCourseTypeId() != null)
                            ? typeDTOMap.get(course.getCourseTypeId()) : null;
                    EmployeeDTO employeeDTO = employeeMap.get(dto.getInitiatingOfficer());
                    Status status = statusMap.get(dto.getStatus());

                    if (course != null) {
                        dto.setCourseName(course.getCourseName());
                        dto.setCourseLevel(course.getCourseLevel());
                        dto.setVenue(course.getVenue());
                        dto.setOfflineRegistrationFee(course.getOfflineRegistrationFee());
                        dto.setOnlineRegistrationFee(course.getOnlineRegistrationFee());
                    }

                    if (typeDTO != null) {
                        dto.setCourseType(typeDTO.getCourseType());
                    }

                    if (status != null) {
                        dto.setStatusColor(status.getColorCode());
                        dto.setStatusName(status.getStatusName());
                    }

                    if (organizer != null) {
                        dto.setOrganizer(organizer.getOrganizer());
                        dto.setOrganizerContactName(organizer.getContactName());
                        dto.setOrganizerPhoneNo(organizer.getPhoneNo());
                        dto.setOrganizerFaxNo(organizer.getFaxNo());
                        dto.setOrganizerEmail(organizer.getEmail());
                    }

                    if (employeeDTO != null) {
                        dto.setEmpNo(employeeDTO.getEmpNo());
                        dto.setInitiatingOfficerName(CommonUtil.buildEmployeeName(employeeDTO, false));
                        dto.setDesigCadre(employeeDTO.getDesigCadre());
                        dto.setEmpDesigName(employeeDTO.getEmpDesigName());
                        dto.setEmpDivCode(employeeDTO.getEmpDivCode());
                        dto.setEmail(employeeDTO.getEmail());
                        dto.setMobileNo(employeeDTO.getMobileNo());
                    }
                })
                .sorted((dto1, dto2) -> {
                    EmployeeDTO emp1 = employeeMap.get(dto1.getInitiatingOfficer());
                    EmployeeDTO emp2 = employeeMap.get(dto2.getInitiatingOfficer());

                    Integer sr1 = (emp1 != null) ? emp1.getSrNo().intValue() : null;
                    Integer sr2 = (emp2 != null) ? emp2.getSrNo().intValue() : null;

                    boolean isEnd1 = (sr1 == null || sr1 <= 0);
                    boolean isEnd2 = (sr2 == null || sr2 <= 0);

                    if (isEnd1 && isEnd2) return 0;
                    if (isEnd1) return 1;   // Move dto1 (0 or null) to the end
                    if (isEnd2) return -1;  // Keep dto2 (0 or null) at the end

                    return Integer.compare(sr1, sr2); // Ascending order for valid srNo (1, 2, 3...)
                })
                .toList();
    }

    @Cacheable(value = "cepReportCache", key = "#fromDate + '_' + #toDate + '_' + #username")
    public List<CepDTO> getCEPData(LocalDate fromDate, LocalDate toDate, String username) {
        log.info("Fetching CEP list data from {} to {}", fromDate, toDate);

        List<Cep> cepList = cepRepository.getCepDataByDateRange(fromDate, toDate);
        List<CepDTO> dtoList = cepMapper.toDto(cepList);

        Map<Long, DivisionDTO> divisionDTOMap = masterCacheService.getDivisionDTOMap();
        Map<Long, EmployeeDTO> employeeDTOMap = masterCacheService.getLongEmployeeDTOMap();

        dtoList.forEach(data -> {

            DivisionDTO divisionDTO = divisionDTOMap.get(data.getDivisionId());
            EmployeeDTO coordinator = employeeDTOMap.get(data.getCourseCoordinatorId());
            EmployeeDTO deputyCoordinator = employeeDTOMap.get(data.getDeputyCourseCoordinatorId());

            if (divisionDTO != null) {
                data.setDivisionCode(divisionDTO.getDivisionShortName());
            }

            if (coordinator != null) {
                data.setCourseCoordinatorName(
                        CommonUtil.buildEmployeeName(coordinator, true)
                );
            }

            if (deputyCoordinator != null) {
                data.setDeputyCourseCoordinatorName(
                        CommonUtil.buildEmployeeName(deputyCoordinator, true)
                );
            }
        });
        return dtoList;
    }

    @Cacheable(value = "mtechReportCache", key = "#fromDate + '_' + #toDate")
    public List<SponsorshipDTO> getMTechData(LocalDate fromDate, LocalDate toDate) {
        log.info("Fetching Sponsorship M.Tech list data");

        List<Sponsorship> list = sponsorshipRepository.findByDegreeTypeAndDateRange("MTECH", fromDate, toDate);
        List<SponsorshipDTO> dtoList = sponsorshipMapper.toDto(list);

        Map<Long, EmployeeDTO> employeeDTOMap = masterCacheService.getLongEmployeeDTOMap();

        dtoList.forEach(data -> {
            EmployeeDTO employeeDTO = employeeDTOMap.get(data.getEmpId());
            if (employeeDTO != null) {
                data.setEmpNo(employeeDTO.getEmpNo());
                data.setEmployeeName(CommonUtil.buildEmployeeName(employeeDTO, false));
                data.setEmpDesigCode(employeeDTO.getEmpDesigName());
                data.setDesigCadre(employeeDTO.getDesigCadre());
                data.setEmpDivCode(employeeDTO.getEmpDivCode());
            }
        });

        return dtoList;
    }

    @Cacheable(value = "phdReportCache", key = "#fromDate + '_' + #toDate")
    public List<SponsorshipDTO> getPhdData(LocalDate fromDate, LocalDate toDate) {
        log.info("Fetching Sponsorship Phd list data");

        List<Sponsorship> list = sponsorshipRepository.findByDegreeTypeAndDateRange("PHD", fromDate, toDate);
        List<SponsorshipDTO> dtoList = sponsorshipMapper.toDto(list);

        Map<Long, EmployeeDTO> employeeDTOMap = masterCacheService.getLongEmployeeDTOMap();

        dtoList.forEach(data -> {
            EmployeeDTO employeeDTO = employeeDTOMap.get(data.getEmpId());
            if (employeeDTO != null) {
                data.setEmpNo(employeeDTO.getEmpNo());
                data.setEmployeeName(CommonUtil.buildEmployeeName(employeeDTO, false));
                data.setEmpDesigCode(employeeDTO.getEmpDesigName());
                data.setDesigCadre(employeeDTO.getDesigCadre());
                data.setEmpDivCode(employeeDTO.getEmpDivCode());
            }
        });

        return dtoList;
    }

    @Cacheable(value = "hrDistributionReportCache", key = "#fromDate + '_' + #toDate")
    public List<DistributionDTO> getHrDistributionData(LocalDate fromDate, LocalDate toDate) {
        log.info("Fetching HR distribution list data");

        List<Distribution> list = distributionRepository.findActiveDistributionsByDateRange(fromDate, toDate);
        List<DistributionDTO> dtoList = distributionMapper.toDto(list);

        Map<Long, List<ProjectEmployeeDto>> empProjectMap = masterCacheService.getProjectEmployeeMap();
        Map<Long, EmployeeDTO> employeeDTOMap = masterCacheService.getLongEmployeeDTOMap();

        dtoList.forEach(data -> {

            EmployeeDTO employeeDTO = employeeDTOMap.get(data.getEmpId());
            EmployeeDTO aoEmpDto = employeeDTOMap.get(data.getAoEmpId());
            EmployeeDTO roEmpDto = employeeDTOMap.get(data.getRoEmpId());

            if (employeeDTO != null) {
                data.setEmpNo(employeeDTO.getEmpNo());
                data.setEmployeeName(CommonUtil.buildEmployeeName(employeeDTO, true));
                data.setDesigCadre(employeeDTO.getDesigCadre());
                data.setEmpDivCode(employeeDTO.getEmpDivCode());
            }

            data.setAoOfficerName(aoEmpDto != null ? CommonUtil.buildEmployeeName(aoEmpDto, true) : "");
            data.setRoOfficerName(roEmpDto != null ? CommonUtil.buildEmployeeName(roEmpDto, true) : "");

            List<ProjectEmployeeDto> projectList =
                    empProjectMap.getOrDefault(data.getEmpId(), Collections.emptyList());

            if (projectList != null && !projectList.isEmpty()) {

                List<ProjectRoleDto> roleDtoList = projectList.stream()
                        .map(p -> {
                            ProjectRoleDto dto = new ProjectRoleDto();
                            dto.setProjectCode(p.getProjectCode());
                            dto.setRoleName(p.getRole());
                            return dto;
                        })
                        .collect(Collectors.toList());
                data.setRoleDtoList(roleDtoList);

            } else {
                data.setRoleDtoList(Collections.emptyList());
            }
        });
        return dtoList;
    }

    public List<AnnualTrainingReportDTO> getAnnualTrainingReport(LocalDate fromDate, LocalDate toDate, String token) {

        log.info("Fetching Annual Training Report. From Date: {}, To Date: {}", fromDate, toDate);


        List<Requisition> requisitions = requisitionRepository.getRequisitionDataByDateRange(fromDate, toDate);
        List<Feedback> feedbacks = feedbackRepository.findByDateRange(fromDate, toDate);

        List<Course> courses = courseRepository.findAll();
        List<CourseType> courseTypes = courseTypeRepository.findAll();

        List<EmployeeDTO> employeeDTOList = getNominalRollList(token);

        log.info(
                "Annual Training Report data fetched. " +
                        "Requisitions: {}, Feedbacks: {}, Courses: {}, CourseTypes: {}, Employees: {}",
                requisitions.size(),
                feedbacks.size(),
                courses.size(),
                courseTypes.size(),
                employeeDTOList.size()
        );


        // Requisition.courseId -> Course
        Map<Long, Course> courseMap = courses.stream()
                .filter(Objects::nonNull)
                .filter(course -> course.getCourseId() != null)
                .collect(Collectors.toMap(
                        Course::getCourseId,
                        Function.identity(),
                        (existing, replacement) -> existing
                ));


        // Course.courseTypeId -> CourseType
        Map<Long, CourseType> courseTypeMap = courseTypes.stream()
                .filter(Objects::nonNull)
                .filter(courseType -> courseType.getCourseTypeId() != null)
                .collect(Collectors.toMap(
                        CourseType::getCourseTypeId,
                        Function.identity(),
                        (existing, replacement) -> existing
                ));


        // Feedback.requisitionId -> Requisition
        Map<Long, Requisition> requisitionMap = requisitions.stream()
                .filter(Objects::nonNull)
                .filter(req -> req.getRequisitionId() != null)
                .collect(Collectors.toMap(
                        Requisition::getRequisitionId,
                        Function.identity(),
                        (existing, replacement) -> existing
                ));


        // Requisition.initiatingOfficer -> EmployeeDTO
        Map<Long, EmployeeDTO> employeeMap = employeeDTOList.stream()
                .filter(Objects::nonNull)
                .filter(emp -> emp.getEmpId() != null)
                .collect(Collectors.toMap(
                        EmployeeDTO::getEmpId,
                        Function.identity(),
                        (existing, replacement) -> existing
                ));


        // IMPORTANT:
        // Every CourseType will appear in the report even when
        // there are no participants.
        Map<Long, AnnualTrainingReportDTO> reportMap = new LinkedHashMap<>();

        for (CourseType courseType : courseTypes) {

            if (courseType == null || courseType.getCourseTypeId() == null) {
                log.warn("Skipping CourseType because CourseType ID is null");
                continue;
            }

            AnnualTrainingReportDTO dto = new AnnualTrainingReportDTO();

            dto.setCourseTypeId(courseType.getCourseTypeId());
            dto.setCourse(courseType.getCourseType());

            // Explicitly initialize fields that currently
            dto.setWithinLab(0L);
            dto.setOtherLabs(0L);
            reportMap.put(courseType.getCourseTypeId(), dto);
        }

        log.info("Initialized {} dynamic course type report rows", reportMap.size());


        // =========================================================
        // 7. Process Feedback
        //
        // Feedback
        //      |
        //      | requisitionId
        //      v
        // Requisition
        //      |
        //      | courseId
        //      v
        // Course
        //      |
        //      | courseTypeId
        //      v
        // CourseType / Report Row
        //
        // Requisition.initiatingOfficer
        //      |
        //      v
        // EmployeeDTO
        //      |
        //      +-- desigCadre
        //      |
        //      +-- employeeDetails.gender
        // =========================================================

        int processedFeedbacks = 0;
        int skippedFeedbacks = 0;

        for (Feedback feedback : feedbacks) {

            if (feedback == null) {
                skippedFeedbacks++;
                continue;
            }

            // Feedback -> Requisition
            Long requisitionId = feedback.getRequisitionId();

            if (requisitionId == null) {
                log.debug("Skipping feedback because requisitionId is null");
                skippedFeedbacks++;
                continue;
            }

            Requisition requisition = requisitionMap.get(requisitionId);
            if (requisition == null) {
                log.debug("Skipping feedback. Requisition not found for ID: {}", requisitionId
                );
                skippedFeedbacks++;
                continue;
            }


            // Requisition -> Course
            Long courseId = requisition.getCourseId();
            if (courseId == null) {
                log.debug("Skipping requisition {} because courseId is null", requisitionId
                );
                skippedFeedbacks++;
                continue;
            }

            Course course = courseMap.get(courseId);
            if (course == null) {
                log.debug("Skipping requisition {}. Course not found for courseId: {}", requisitionId, courseId);
                skippedFeedbacks++;
                continue;
            }

            // Course -> CourseType
            Long courseTypeId = course.getCourseTypeId();
            if (courseTypeId == null) {
                log.debug("Skipping course {} because courseTypeId is null", courseId);
                skippedFeedbacks++;
                continue;
            }

            CourseType courseType = courseTypeMap.get(courseTypeId);
            if (courseType == null) {
                log.debug("Skipping course {}. CourseType not found for ID: {}", courseId, courseTypeId);
                skippedFeedbacks++;
                continue;
            }

            // Find report row
            AnnualTrainingReportDTO dto = reportMap.get(courseTypeId);
            if (dto == null) {
                log.debug("Skipping feedback {}. Report row not found for courseTypeId: {}", feedback.getRequisitionId(), courseTypeId);
                skippedFeedbacks++;
                continue;
            }


            // Requisition -> Employee
            Long employeeId = requisition.getInitiatingOfficer();
            if (employeeId == null) {
                log.debug("Skipping requisition {} because initiatingOfficer is null", requisitionId);
                skippedFeedbacks++;
                continue;
            }


            EmployeeDTO employee = employeeMap.get(employeeId);
            if (employee == null) {
                log.debug("Skipping requisition {}. Employee not found for employeeId: {}", requisitionId, employeeId);
                skippedFeedbacks++;
                continue;
            }

            // Get cadre
            String cadre = normalize(employee.getDesigCadre());

            // Get gender
            String gender = "";
            if (employee.getEmployeeDetails() != null) {
                gender = normalize(employee.getEmployeeDetails().getGender());
            }

            // Get requestThrough
            String requestThrough = normalize(feedback.getRequestThrough());


            // Update course level counts
            // National      -> Outside Within India
            // International -> Outside Foreign Agency
            // Within Lab    -> 0
            // Other Labs    -> 0
            updateCourseLevelCount(dto, course.getCourseLevel());


            // Sub Total
            // Every valid feedback represents one participant
            dto.setSubTotal(increment(dto.getSubTotal()));

            // Update cadre/request-through/gender counts
            updateCadreCounts(dto, cadre, requestThrough, gender);
            processedFeedbacks++;
        }

        log.info(
                "Annual Training Report processing completed. " +
                        "Processed feedbacks: {}, Skipped feedbacks: {}, " +
                        "Report rows: {}",
                processedFeedbacks,
                skippedFeedbacks,
                reportMap.size()
        );

        if (log.isDebugEnabled()) {
            reportMap.values().forEach(dto ->
                    log.debug(
                            "Annual Training Report row: courseTypeId={}, course={}, " +
                                    "subTotal={}, DRDS={}, DRTC={}, AdminAllied={}, Others={}",
                            dto.getCourseTypeId(),
                            dto.getCourse(),
                            dto.getSubTotal(),
                            dto.getDrdsTotal(),
                            dto.getDrtcTotal(),
                            dto.getAdminAlliedTotal(),
                            dto.getOthersTotal()
                    )
            );
        }

        return new ArrayList<>(reportMap.values());
    }

    private void updateCourseLevelCount(AnnualTrainingReportDTO dto, String courseLevel) {
        String level = normalize(courseLevel);
        if ("NATIONAL".equalsIgnoreCase(level)) {
            dto.setOutsideWithinIndia(increment(dto.getOutsideWithinIndia()));
        } else if ("INTERNATIONAL".equalsIgnoreCase(level)) {
            dto.setOutsideForeignAgency(increment(dto.getOutsideForeignAgency()));
        } else {
            log.debug("Course level '{}' is not mapped to National/International", courseLevel);
        }
        // These currently have no source field/data.
        dto.setWithinLab(0L);
        dto.setOtherLabs(0L);
    }

    private void updateCadreCounts(AnnualTrainingReportDTO dto, String cadre, String requestThrough, String gender) {

        // DRDS
        if ("DRDS".equalsIgnoreCase(cadre)) {
            updateDrdsCounts(dto, requestThrough, gender);
            return;
        }

        // DRTC
        if ("DRTC".equalsIgnoreCase(cadre)) {
            updateDrtcCounts(dto, requestThrough, gender);
            return;
        }

        // ADMIN & ALLIED
        if ("ADMIN & ALLIED".equalsIgnoreCase(cadre)) {
            updateAdminAlliedCounts(dto, requestThrough, gender);
            return;
        }

        // OTHERS
        updateOthersCounts(dto, gender);
    }

    private void updateDrdsCounts(AnnualTrainingReportDTO dto, String requestThrough,
                                  String gender) {
        // Request Through
        if ("TNI".equalsIgnoreCase(requestThrough)) {
            dto.setDrdsTni(increment(dto.getDrdsTni()));
        } else if ("RAC BOARD".equalsIgnoreCase(requestThrough)) {
            dto.setDrdsRacBoard(increment(dto.getDrdsRacBoard()));
        } else if ("OPEN".equalsIgnoreCase(requestThrough)) {
            dto.setDrdsOpen(increment(dto.getDrdsOpen()));
        } else {
            log.debug("Unknown DRDS requestThrough: {}", requestThrough);
        }

        // Total
        dto.setDrdsTotal(increment(dto.getDrdsTotal()));

        // Gender
        updateDrdsGender(dto, gender);
    }

    private void updateDrdsGender(AnnualTrainingReportDTO dto, String gender) {
        if ("MALE".equalsIgnoreCase(gender)) {
            dto.setDrdsMale(increment(dto.getDrdsMale()));
        } else if ("FEMALE".equalsIgnoreCase(gender)) {
            dto.setDrdsFemale(increment(dto.getDrdsFemale()));
        } else {
            log.debug("Unknown DRDS gender: {}", gender);
        }
    }

    private void updateDrtcCounts(AnnualTrainingReportDTO dto, String requestThrough, String gender) {

        // Request Through
        if ("TNI".equalsIgnoreCase(requestThrough)) {
            dto.setDrtcTni(increment(dto.getDrtcTni()));
        } else if ("CEPTAM BOARD".equalsIgnoreCase(requestThrough)) {
            dto.setDrtcCeptamBoard(increment(dto.getDrtcCeptamBoard()));
        } else if ("OPEN".equalsIgnoreCase(requestThrough)) {
            dto.setDrtcOpen(increment(dto.getDrtcOpen()));
        } else {
            log.debug("Unknown DRTC requestThrough: {}", requestThrough);
        }

        // Total
        dto.setDrtcTotal(increment(dto.getDrtcTotal()));

        // Gender
        if ("MALE".equalsIgnoreCase(gender)) {
            dto.setDrtcMale(increment(dto.getDrtcMale()));
        } else if ("FEMALE".equalsIgnoreCase(gender)) {
            dto.setDrtcFemale(increment(dto.getDrtcFemale()));
        } else {
            log.debug("Unknown DRTC gender: {}", gender);
        }
    }

    private void updateAdminAlliedCounts(AnnualTrainingReportDTO dto, String requestThrough, String gender) {

        // Request Through
        if ("TNI".equalsIgnoreCase(requestThrough)) {
            dto.setAdminAlliedTni(increment(dto.getAdminAlliedTni()));
        } else if ("OPEN".equalsIgnoreCase(requestThrough)) {
            dto.setAdminAlliedOpen(increment(dto.getAdminAlliedOpen()));
        } else {
            log.debug("Admin & Allied requestThrough '{}' has no dedicated column", requestThrough);
        }

        // Total
        dto.setAdminAlliedTotal(increment(dto.getAdminAlliedTotal()));

        // Gender
        if ("MALE".equalsIgnoreCase(gender)) {
            dto.setAdminAlliedMale(increment(dto.getAdminAlliedMale()));
        } else if ("FEMALE".equalsIgnoreCase(gender)) {
            dto.setAdminAlliedFemale(increment(dto.getAdminAlliedFemale()));
        } else {
            log.debug("Unknown Admin & Allied gender: {}", gender);
        }
    }

    private void updateOthersCounts(AnnualTrainingReportDTO dto, String gender) {
        // Total
        dto.setOthersTotal(increment(dto.getOthersTotal()));

        // Gender
        if ("MALE".equalsIgnoreCase(gender)) {
            dto.setOthersMale(increment(dto.getOthersMale()));
        } else if ("FEMALE".equalsIgnoreCase(gender)) {
            dto.setOthersFemale(increment(dto.getOthersFemale()));
        } else {
            log.debug("Unknown Others gender: {}", gender);
        }
    }

    private Long increment(Long value) {
        return value == null ? 1L : value + 1L;
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim();
    }


    public List<BudgetExpenditureDTO> getBudgetExpenditureReport(LocalDate fromDate, LocalDate toDate, String username) {
        log.info("Fetching budget expenditure report data");

        List<Requisition> requisitions = requisitionRepository.getRequisitionDataByDateRange(fromDate, toDate);
        List<Course> courses = courseRepository.findAll();
        List<CourseType> courseTypes = courseTypeRepository.findAll();
        List<Sponsorship> sponsorships = sponsorshipRepository.getSponsorshipDataByDateRange(fromDate, toDate);

        // Convert to Map for fast lookup
        Map<Long, Course> courseMap = courses.stream()
                .collect(Collectors.toMap(Course::getCourseId, Function.identity()));

        Map<Long, CourseType> courseTypeMap = courseTypes.stream()
                .collect(Collectors.toMap(CourseType::getCourseTypeId, Function.identity()));

        Map<Long, List<Sponsorship>> sponsorshipMap = sponsorships.stream()
                .collect(Collectors.groupingBy(Sponsorship::getEmpId));

        // Group requisitions by empId
        Map<Long, List<Requisition>> requisitionByEmp =
                requisitions.stream()
                        .filter(e->e.getIsAttend().equalsIgnoreCase("Y"))
                        .collect(Collectors.groupingBy(Requisition::getInitiatingOfficer));

        List<BudgetExpenditureDTO> result = new ArrayList<>();
        Map<Long, EmployeeDTO> employeeMap = masterCacheService.getLongEmployeeDTOMap();

        for (Map.Entry<Long, List<Requisition>> entry : requisitionByEmp.entrySet()) {

            Long empId = entry.getKey();
            List<Requisition> empRequisitions = entry.getValue();

            BudgetExpenditureDTO dto = new BudgetExpenditureDTO();

            EmployeeDTO employeeDTO = employeeMap.get(empId);
            dto.setEmpId(empId);
            dto.setEmpName(employeeDTO != null ? CommonUtil.buildEmployeeName(employeeDTO, true) : "");

            // Initialize all BigDecimals as ZERO
            dto.setCep(BigDecimal.ZERO);
            dto.setSpecialRE(BigDecimal.ZERO);
            dto.setSpecialFE(BigDecimal.ZERO);
            dto.setTargetedRE(BigDecimal.ZERO);
            dto.setTargetedFE(BigDecimal.ZERO);
            dto.setMeRt(BigDecimal.ZERO);
            dto.setMeDirector(BigDecimal.ZERO);
            dto.setTechManagerial(BigDecimal.ZERO);
            dto.setForeignRE(BigDecimal.ZERO);
            dto.setForeignFE(BigDecimal.ZERO);
            dto.setPhd(BigDecimal.ZERO);
            dto.setRegistrationRE(BigDecimal.ZERO);
            dto.setRegistrationFE(BigDecimal.ZERO);
            dto.setCourseFee(BigDecimal.ZERO);
            dto.setOthersRE(BigDecimal.ZERO);
            dto.setOthersFE(BigDecimal.ZERO);

            for (Requisition req : empRequisitions) {

                Course course = courseMap.get(req.getCourseId());
                if (course == null) continue;

                CourseType courseType = courseTypeMap.get(course.getCourseTypeId());
                if (courseType == null) continue;

                BigDecimal regFee =
                        Optional.ofNullable(req.getRegistrationFee()).orElse(BigDecimal.ZERO);

                String type = courseType.getCourseType();
                String level = course.getCourseLevel();

                // 1. CEP
                if ("CEP".equalsIgnoreCase(type)) {
                    dto.setCep(dto.getCep().add(regFee));
                }

                // 2. Special
                else if ("Special".equalsIgnoreCase(type)) {
                    if ("National".equalsIgnoreCase(level)) {
                        dto.setSpecialRE(dto.getSpecialRE().add(regFee));
                    } else {
                        dto.setSpecialFE(dto.getSpecialFE().add(regFee));
                    }
                }

                // 4. Techno Managerial
                else if ("Techno Managerial".equalsIgnoreCase(type)) {
                    dto.setTechManagerial(dto.getTechManagerial().add(regFee));
                }

                // 5. Training
                else if ("Training".equalsIgnoreCase(type)) {
                    if ("National".equalsIgnoreCase(level)) {
                        dto.setForeignRE(dto.getForeignRE().add(regFee));
                    } else {
                        dto.setForeignFE(dto.getForeignFE().add(regFee));
                    }
                }

                // 7. Seminar/Symposium/Conference/Workshop
                else if (Stream.of("Seminar", "Symposium", "Conference", "Workshop").anyMatch(type::equalsIgnoreCase)) {

                    if ("National".equalsIgnoreCase(level)) {
                        dto.setRegistrationRE(dto.getRegistrationRE().add(regFee));
                    } else {
                        dto.setRegistrationFE(dto.getRegistrationFE().add(regFee));
                    }
                }

                // 8. Course Fee (offlineRegistrationFee)
                if (course.getOfflineRegistrationFee() != null) {
                    dto.setCourseFee(dto.getCourseFee()
                            .add(course.getOfflineRegistrationFee()));
                }
            }

            // 3 & 6 Sponsorship Logic
            List<Sponsorship> empSponsorships =
                    sponsorshipMap.getOrDefault(empId, Collections.emptyList());

            for (Sponsorship sp : empSponsorships) {

                BigDecimal exp = Optional.ofNullable(sp.getExpenditure())
                        .orElse(BigDecimal.ZERO);

                if ("MTECH".equalsIgnoreCase(sp.getDegreeType())) {
                    dto.setMeRt(dto.getMeRt().add(exp));
                    dto.setMeDirector(BigDecimal.ZERO);
                }

                if ("PHD".equalsIgnoreCase(sp.getDegreeType())) {
                    dto.setPhd(dto.getPhd().add(exp));
                }
            }

            // 9 Others already ZERO

            // 10 Total Calculation
            BigDecimal total = Stream.of(
                    dto.getCep(),
                    dto.getSpecialRE(),
                    dto.getSpecialFE(),
                    dto.getTargetedRE(),
                    dto.getTargetedFE(),
                    dto.getMeRt(),
                    dto.getMeDirector(),
                    dto.getTechManagerial(),
                    dto.getForeignRE(),
                    dto.getForeignFE(),
                    dto.getPhd(),
                    dto.getRegistrationRE(),
                    dto.getRegistrationFE(),
                    dto.getCourseFee(),
                    dto.getOthersRE(),
                    dto.getOthersFE()
            ).reduce(BigDecimal.ZERO, BigDecimal::add);

            dto.setTotal(total);

            result.add(dto);
        }

        return result;
    }

    public List<GenderBudgetDTO> getGenderBudgetReport(String token) {
        log.info("Fetching gender budget report data");
        List<Requisition> requisitions = requisitionRepository.findAll();
        List<Course> courses = courseRepository.findAll();
        List<CourseType> courseTypes = courseTypeRepository.findAll();
        List<Sponsorship> sponsorships = sponsorshipRepository.findAll();
        List<EmployeeDTO> employeeDTOList = getNominalRollList(token);

        Map<Long, EmployeeDTO> employeeMap = employeeDTOList.stream().filter(Objects::nonNull).filter(e -> e.getEmpId() != null).collect(Collectors.toMap(EmployeeDTO::getEmpId, Function.identity(), (existing, replacement) -> existing));
        Map<Long, Course> courseMap = courses.stream().filter(Objects::nonNull).filter(c -> c.getCourseId() != null).collect(Collectors.toMap(Course::getCourseId, Function.identity(), (existing, replacement) -> existing));
        Map<Long, CourseType> courseTypeMap = courseTypes.stream().filter(Objects::nonNull).filter(c -> c.getCourseTypeId() != null).collect(Collectors.toMap(CourseType::getCourseTypeId, Function.identity(), (existing, replacement) -> existing));

        GenderBudgetDTO dto = new GenderBudgetDTO();
        dto.setLabName(labCode);

        initializeZero(dto);

        Set<Long> cepMaleEmp = new HashSet<>();
        Set<Long> cepFemaleEmp = new HashSet<>();
        Set<Long> specialMaleEmp = new HashSet<>();
        Set<Long> specialFemaleEmp = new HashSet<>();
        Set<Long> seminarMaleEmp = new HashSet<>();
        Set<Long> seminarFemaleEmp = new HashSet<>();
        Set<Long> foreignMaleEmp = new HashSet<>();
        Set<Long> foreignFemaleEmp = new HashSet<>();
        Set<Long> otherMaleEmp = new HashSet<>();
        Set<Long> otherFemaleEmp = new HashSet<>();

        for (Requisition req : requisitions) {
            if (req == null || req.getCourseId() == null) {
                continue;
            }
            Course course = courseMap.get(req.getCourseId());
            if (course == null) {
                log.debug("Course not found for requisition. CourseId={}", req.getCourseId());
                continue;
            }
            CourseType type = courseTypeMap.get(course.getCourseTypeId());
            if (type == null) {
                log.debug("Course type not found. CourseId={}, CourseTypeId={}", course.getCourseId(), course.getCourseTypeId());
                addEmployeeToCategory(req.getInitiatingOfficer(), employeeMap, otherMaleEmp, otherFemaleEmp);
                continue;
            }
            Long empId = req.getInitiatingOfficer();
            if (empId == null) {
                log.debug("Initiating employee is null. RequisitionId={}", req.getRequisitionId());
                continue;
            }
            String courseType = type.getCourseType();
            String level = course.getCourseLevel();
            if ("CEP".equalsIgnoreCase(courseType)) {
                addEmployeeToCategory(empId, employeeMap, cepMaleEmp, cepFemaleEmp);
            } else if ("Special".equalsIgnoreCase(courseType)) {
                addEmployeeToCategory(empId, employeeMap, specialMaleEmp, specialFemaleEmp);
            } else if (isSeminarType(courseType)) {
                addEmployeeToCategory(empId, employeeMap, seminarMaleEmp, seminarFemaleEmp);
            } else if ("Training".equalsIgnoreCase(courseType) && "International".equalsIgnoreCase(level)) {
                addEmployeeToCategory(empId, employeeMap, foreignMaleEmp, foreignFemaleEmp);
            } else {
                addEmployeeToCategory(empId, employeeMap, otherMaleEmp, otherFemaleEmp);
            }
        }
        setGenderValues(dto, cepMaleEmp.size(), cepFemaleEmp.size(), GenderCategory.CEP);
        setGenderValues(dto, specialMaleEmp.size(), specialFemaleEmp.size(), GenderCategory.SPECIAL);
        setGenderValues(dto, seminarMaleEmp.size(), seminarFemaleEmp.size(), GenderCategory.SEMINAR);
        setGenderValues(dto, foreignMaleEmp.size(), foreignFemaleEmp.size(), GenderCategory.FOREIGN);
        setGenderValues(dto, otherMaleEmp.size(), otherFemaleEmp.size(), GenderCategory.OTHERS);

        int higherDegreeTotal = sponsorships.size();
        dto.setHigherDegreeTotalNo(BigDecimal.valueOf(higherDegreeTotal));
        dto.setHigherDegreeMaleExp(BigDecimal.ZERO);
        dto.setHigherDegreeFemaleExp(BigDecimal.ZERO);
        dto.setHigherDegreeTotalExp(BigDecimal.ZERO);
        log.info("Gender budget report generated. Lab={}, CEP={}, Special={}, " + "Seminar={}, Foreign={}, Others={}, HigherDegree={}", labCode, dto.getCepTotalNo(), dto.getSpecialTotalNo(), dto.getSeminarTotalNo(), dto.getForeignTotalNo(), dto.getOthersTotalNo(), dto.getHigherDegreeTotalNo());
        return Collections.singletonList(dto);
    }


    private void addEmployeeToCategory(Long empId, Map<Long, EmployeeDTO> employeeMap, Set<Long> maleEmployees, Set<Long> femaleEmployees) {
        if (empId == null) {
            return;
        }

        EmployeeDTO employee = employeeMap.get(empId);
        if (employee == null) {
            log.debug("Employee not found in nominal roll. EmpId={}", empId);
            return;
        }

        if (employee.getEmployeeDetails() == null) {
            log.debug("Employee details not available. EmpId={}", empId);
            return;
        }

        String gender = employee.getEmployeeDetails().getGender();
        if (gender == null || gender.trim().isEmpty()) {
            log.debug("Gender not available for employee. EmpId={}", empId);
            return;
        }

        gender = gender.trim();

        if ("Male".equalsIgnoreCase(gender)) {
            maleEmployees.add(empId);
        } else if ("Female".equalsIgnoreCase(gender)) {
            femaleEmployees.add(empId);
        } else {
            log.debug("Unsupported gender '{}' for employee. EmpId={}", gender, empId
            );
        }
    }

    private boolean isSeminarType(String courseType) {
        if (courseType == null || courseType.trim().isEmpty()) {
            return false;
        }
        return Stream.of("Seminar", "Symposium", "Conference", "Workshop").anyMatch(courseType::equalsIgnoreCase);
    }

    private void setGenderValues(GenderBudgetDTO dto, int maleCount, int femaleCount, GenderCategory category) {
        BigDecimal male = BigDecimal.valueOf(maleCount);
        BigDecimal female = BigDecimal.valueOf(femaleCount);
        BigDecimal total = male.add(female);
        switch (category) {
            case CEP:
                dto.setCepMaleNo(male);
                dto.setCepFemaleNo(female);
                dto.setCepTotalNo(total);
                dto.setCepMaleExp(BigDecimal.ZERO);
                dto.setCepFemaleExp(BigDecimal.ZERO);
                dto.setCepTotalExp(BigDecimal.ZERO);
                break;
            case SPECIAL:
                dto.setSpecialMaleNo(male);
                dto.setSpecialFemaleNo(female);
                dto.setSpecialTotalNo(total);
                dto.setSpecialMaleExp(BigDecimal.ZERO);
                dto.setSpecialFemaleExp(BigDecimal.ZERO);
                dto.setSpecialTotalExp(BigDecimal.ZERO);
                break;
            case SEMINAR:
                dto.setSeminarMaleNo(male);
                dto.setSeminarFemaleNo(female);
                dto.setSeminarTotalNo(total);
                dto.setSeminarMaleExp(BigDecimal.ZERO);
                dto.setSeminarFemaleExp(BigDecimal.ZERO);
                dto.setSeminarTotalExp(BigDecimal.ZERO);
                break;
            case FOREIGN:
                dto.setForeignMaleNo(male);
                dto.setForeignFemaleNo(female);
                dto.setForeignTotalNo(total);
                dto.setForeignMaleExp(BigDecimal.ZERO);
                dto.setForeignFemaleExp(BigDecimal.ZERO);
                dto.setForeignTotalExp(BigDecimal.ZERO);
                break;
            case OTHERS:
                dto.setOthersMaleNo(male);
                dto.setOthersFemaleNo(female);
                dto.setOthersTotalNo(total);
                dto.setOthersMaleExp(BigDecimal.ZERO);
                dto.setOthersFemaleExp(BigDecimal.ZERO);
                dto.setOthersTotalExp(BigDecimal.ZERO);
                break;
            default:
                break;
        }
    }

    private enum GenderCategory {CEP, SPECIAL, SEMINAR, FOREIGN, OTHERS}

    public List<TrainingSCSTDTO> getTrainingSCSTReport(String token, String username) {
        log.info("Fetching Training SC/ST report data");
        List<EmployeeDTO> employeeDTOList = getNominalRollList(token);
        List<Requisition> requisitions = requisitionRepository.findAll();
        List<Course> courses = courseRepository.findAll();
        List<CourseType> courseTypes = courseTypeRepository.findAll();
        Map<Long, EmployeeDTO> employeeMap = employeeDTOList.stream().filter(Objects::nonNull).filter(e -> e.getEmpId() != null).collect(Collectors.toMap(EmployeeDTO::getEmpId, Function.identity(), (existing, replacement) -> existing)); /* * ------------------------------------------------------------ * Course Map * ------------------------------------------------------------ */
        Map<Long, Course> courseMap = courses.stream().filter(Objects::nonNull).filter(c -> c.getCourseId() != null).collect(Collectors.toMap(Course::getCourseId, Function.identity(), (existing, replacement) -> existing)); /* * ------------------------------------------------------------ * Course Type Map * ------------------------------------------------------------ */
        Map<Long, CourseType> courseTypeMap = courseTypes.stream().filter(Objects::nonNull).filter(c -> c.getCourseTypeId() != null).collect(Collectors.toMap(CourseType::getCourseTypeId, Function.identity(), (existing, replacement) -> existing)); /* * ------------------------------------------------------------ * DTO * ------------------------------------------------------------ */
        TrainingSCSTDTO dto = new TrainingSCSTDTO();
        dto.setLabName(labCode);
        initializeTrainingDtoZero(dto);

        Set<Long> cepSet = new HashSet<>();
        Set<Long> specialSet = new HashSet<>();
        Set<Long> seminarSet = new HashSet<>();
        Set<Long> foreignSet = new HashSet<>();
        Set<Long> othersSet = new HashSet<>();

        for (Requisition req : requisitions) {
            if (req == null || req.getInitiatingOfficer() == null) {
                continue;
            }
            Long empId = req.getInitiatingOfficer();
            EmployeeDTO employee = employeeMap.get(empId);
            if (employee == null) {
                log.debug("Employee not found in nominal roll. EmpId={}", empId);
                continue;
            } /* * EmployeeDetails can be null. */
            if (employee.getEmployeeDetails() == null) {
                log.debug("Employee details not available. EmpId={}", empId);
                continue;
            }
            String category = employee.getEmployeeDetails().getCategory();
            if (category == null || category.trim().isEmpty()) {
                log.debug("Employee category not available. EmpId={}", empId);
                continue;
            }
            category = category.trim(); /* * Only SC and ST employees are considered. */
            if (!"SC".equalsIgnoreCase(category) && !"ST".equalsIgnoreCase(category)) {
                continue;
            }
            if (req.getCourseId() == null) {
                log.debug("CourseId not available for requisition. EmpId={}", empId);
                continue;
            }
            Course course = courseMap.get(req.getCourseId());
            if (course == null) {
                log.debug("Course not found. CourseId={}, EmpId={}", req.getCourseId(), empId);
                continue;
            }
            CourseType type = courseTypeMap.get(course.getCourseTypeId());
            if (type == null) {
                othersSet.add(empId);
                continue;
            }
            String courseType = type.getCourseType();
            String level = course.getCourseLevel();
            if ("CEP".equalsIgnoreCase(courseType)) {
                cepSet.add(empId);
            } else if ("Special".equalsIgnoreCase(courseType)) {
                specialSet.add(empId);
            } else if (isSeminarType(courseType)) {
                seminarSet.add(empId);
            } else if ("Training".equalsIgnoreCase(courseType) && "International".equalsIgnoreCase(level)) {
                foreignSet.add(empId);
            } else {
                othersSet.add(empId);
            }
        }
        dto.setCepScstTrained((long) cepSet.size());
        dto.setSpecialTargetedScstTrained((long) specialSet.size());
        dto.setSponsoredSeminarScstTrained((long) seminarSet.size());
        dto.setForeignTrainingScstTrained((long) foreignSet.size());
        dto.setOthersScstTrained((long) othersSet.size());
        dto.setCepExpenditure(BigDecimal.ZERO);
        dto.setSpecialTargetedExpenditure(BigDecimal.ZERO);
        dto.setHigherDegreeExpenditure(BigDecimal.ZERO);
        dto.setSponsoredSeminarExpenditure(BigDecimal.ZERO);
        dto.setForeignTrainingExpenditure(0L);
        dto.setOthersExpenditure(0L);
        dto.setCepPercentage(0L);
        dto.setSpecialTargetedPercentage(0L);
        dto.setHigherDegreePercentage(0L);
        dto.setSponsoredSeminarPercentage(0L);
        dto.setForeignTrainingPercentage(0L);
        dto.setOthersPercentage(0L);

        log.info("Training SC/ST report generated. Lab={}, " + "CEP={}, Special={}, Seminar={}, Foreign={}, Others={}", labCode, dto.getCepScstTrained(), dto.getSpecialTargetedScstTrained(), dto.getSponsoredSeminarScstTrained(), dto.getForeignTrainingScstTrained(), dto.getOthersScstTrained());
        return Collections.singletonList(dto);
    }

    private void initializeTrainingDtoZero(TrainingSCSTDTO dto) {

        dto.setCepScstTrained(0L);
        dto.setCepExpenditure(BigDecimal.ZERO);
        dto.setCepPercentage(0L);

        dto.setSpecialTargetedScstTrained(0L);
        dto.setSpecialTargetedExpenditure(BigDecimal.ZERO);
        dto.setSpecialTargetedPercentage(0L);

        dto.setHigherDegreeScstTrained(0L);
        dto.setHigherDegreeExpenditure(BigDecimal.ZERO);
        dto.setHigherDegreePercentage(0L);

        dto.setSponsoredSeminarScstTrained(0L);
        dto.setSponsoredSeminarExpenditure(BigDecimal.ZERO);
        dto.setSponsoredSeminarPercentage(0L);

        dto.setForeignTrainingScstTrained(0L);
        dto.setForeignTrainingExpenditure(0L);
        dto.setForeignTrainingPercentage(0L);

        dto.setOthersScstTrained(0L);
        dto.setOthersExpenditure(0L);
        dto.setOthersPercentage(0L);
    }

    private void initializeZero(GenderBudgetDTO dto) {

        BigDecimal zero = BigDecimal.ZERO;

        dto.setCepMaleNo(zero);
        dto.setCepMaleExp(zero);
        dto.setCepFemaleNo(zero);
        dto.setCepFemaleExp(zero);
        dto.setCepTotalNo(zero);
        dto.setCepTotalExp(zero);

        dto.setSpecialMaleNo(zero);
        dto.setSpecialMaleExp(zero);
        dto.setSpecialFemaleNo(zero);
        dto.setSpecialFemaleExp(zero);
        dto.setSpecialTotalNo(zero);
        dto.setSpecialTotalExp(zero);

        dto.setHigherDegreeMaleNo(zero);
        dto.setHigherDegreeMaleExp(zero);
        dto.setHigherDegreeFemaleNo(zero);
        dto.setHigherDegreeFemaleExp(zero);
        dto.setHigherDegreeTotalNo(zero);
        dto.setHigherDegreeTotalExp(zero);

        dto.setSeminarMaleNo(zero);
        dto.setSeminarMaleExp(zero);
        dto.setSeminarFemaleNo(zero);
        dto.setSeminarFemaleExp(zero);
        dto.setSeminarTotalNo(zero);
        dto.setSeminarTotalExp(zero);

        dto.setForeignMaleNo(zero);
        dto.setForeignMaleExp(zero);
        dto.setForeignFemaleNo(zero);
        dto.setForeignFemaleExp(zero);
        dto.setForeignTotalNo(zero);
        dto.setForeignTotalExp(zero);

        dto.setOthersMaleNo(zero);
        dto.setOthersMaleExp(zero);
        dto.setOthersFemaleNo(zero);
        dto.setOthersFemaleExp(zero);
        dto.setOthersTotalNo(zero);
        dto.setOthersTotalExp(zero);
    }

    public List<JournalDTO> getResearchPaperIntReport(String username) {
        log.info("Fetching research paper report data");
        return trainingService.getJournalList(0L, "ROLE_ADMIN", username);
    }

    public List<RequisitionDTO> getResearchPaperDetailReport(LocalDate fromDate, LocalDate toDate, String
            username) {
        log.info("Fetching research paper detail report data");

        List<Requisition> requisitions = requisitionRepository.findActiveRequisitionsWithJournalId(fromDate, toDate);

        List<JournalDTO> journalDTOList = trainingService.getJournalList(0L, "ROLE_ADMIN", username);
        Map<Long, JournalDTO> journalMap = journalDTOList.stream()
                .collect(Collectors.toMap(JournalDTO::getJournalId, Function.identity()));

        Map<Long, EmployeeDTO> employeeMap = masterCacheService.getLongEmployeeDTOMap();
        Map<Long, Organizer> organizerMap = masterCacheService.getOrganizerMap();
        Map<Long, Course> courseMap = masterCacheService.getCourseMap();

        List<CourseTypeDTO> typeDTOList = trainingService.getCourseTypeList(username);
        Map<Long, CourseTypeDTO> typeDTOMap = typeDTOList.stream()
                .collect(Collectors.toMap(CourseTypeDTO::getCourseTypeId, Function.identity()));

        return requisitions.stream()
                .map(requisitionMapper::toDto)
                .peek(dto -> {

                    JournalDTO journalDTO = journalMap.get(dto.getJournalId());
                    Course course = courseMap.get(dto.getCourseId());
                    Organizer organizer = organizerMap.get(course.getOrganizerId());
                    CourseTypeDTO typeDTO = typeDTOMap.get(course.getCourseTypeId());
                    EmployeeDTO employeeDTO = employeeMap.get(dto.getInitiatingOfficer());

                    dto.setCourseName(course.getCourseName());
                    dto.setCourseLevel(course.getCourseLevel());
                    dto.setCourseType(typeDTO.getCourseType());
                    dto.setVenue(course.getVenue());

                    dto.setTitleOfPaper(journalDTO != null ? journalDTO.getTitleOfPaper() : "");

                    if (organizer != null) {
                        dto.setOrganizer(organizer.getOrganizer());
                        dto.setOrganizerContactName(organizer.getContactName());
                        dto.setOrganizerPhoneNo(organizer.getPhoneNo());
                        dto.setOrganizerFaxNo(organizer.getFaxNo());
                        dto.setOrganizerEmail(organizer.getEmail());
                    }
                    if (employeeDTO != null) {
                        dto.setEmpNo(employeeDTO.getEmpNo());
                        dto.setInitiatingOfficerName(CommonUtil.buildEmployeeName(employeeDTO, false));
                        dto.setDesigCadre(employeeDTO.getDesigCadre());
                        dto.setEmpDesigName(employeeDTO.getEmpDesigName());
                        dto.setEmpDivCode(employeeDTO.getEmpDivCode());
                        dto.setEmail(employeeDTO.getEmail());
                        dto.setMobileNo(employeeDTO.getMobileNo());
                    }
                })
                .toList();
    }

}
