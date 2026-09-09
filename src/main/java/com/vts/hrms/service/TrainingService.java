package com.vts.hrms.service;

import com.vts.hrms.dto.*;
import com.vts.hrms.entity.*;
import com.vts.hrms.entity.Calendar;
import com.vts.hrms.exception.BadRequestException;
import com.vts.hrms.exception.NotFoundException;
import com.vts.hrms.mapper.*;
import com.vts.hrms.repository.*;
import com.vts.hrms.util.CommonUtil;
import com.vts.hrms.util.FileStorageUtil;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class TrainingService {

    private static final Logger log = LoggerFactory.getLogger(TrainingService.class);

    @Value("${appStorage}")
    private String appStorage;

    @Value("${x_api_key}")
    private String xApiKey;

    @Value("${labCode}")
    private String labCode;

    private final MasterClientService masterClient;
    private final OrganizerRepository organizerRepository;
    private final OrganizerMapper organizerMapper;
    private final CalendarMapper calenderMapper;
    private final CalenderRepository calenderRepository;
    private final CourseMapper courseMapper;
    private final CourseRepository courseRepository;
    private final RequisitionMapper requisitionMapper;
    private final RequisitionRepository requisitionRepository;
    private final FeedbackMapper feedbackMapper;
    private final FeedbackRepository feedbackRepository;
    private final RequisitionTransactionRepository transactionRepository;
    private final MasterCacheService masterCacheService;
    private final NotificationRepository notificationRepository;
    private final SignRoleAuthorityRepository signRoleAuthorityRepository;
    private final RequisitionSequenceRepository sequenceRepository;
    private final EvaluationRepository evaluationRepository;
    private final EligibilityMapper eligibilityMapper;
    private final EligibilityRepository eligibilityRepository;
    private final CourseTypeRepository courseTypeRepository;
    private final LoginRepository loginRepository;
    private final CepRepository cepRepository;
    private final CepMapper cepMapper;
    private final DistributionRepository distributionRepository;
    private final DistributionMapper distributionMapper;
    private final CepAttachmentsRepository cepAttachmentsRepository;
    private final JournalMapper journalMapper;
    private final JournalRepository journalRepository;
    private final HandingOverRepository handingOverRepository;
    private final CashLimitRepository cashLimitRepository;


    public TrainingService(MasterClientService masterClient, OrganizerRepository organizerRepository, OrganizerMapper organizerMapper, CalendarMapper calenderMapper, CalenderRepository calenderRepository, CourseMapper courseMapper, CourseRepository courseRepository, RequisitionMapper requisitionMapper, RequisitionRepository requisitionRepository, FeedbackMapper feedbackMapper, FeedbackRepository feedbackRepository, RequisitionTransactionRepository transactionRepository, MasterCacheService masterCacheService, NotificationRepository notificationRepository, SignRoleAuthorityRepository signRoleAuthorityRepository, RequisitionSequenceRepository sequenceRepository, EvaluationRepository evaluationRepository, EligibilityMapper eligibilityMapper, EligibilityRepository eligibilityRepository, CourseTypeRepository courseTypeRepository, LoginRepository loginRepository, CepRepository cepRepository, CepMapper cepMapper, DistributionRepository distributionRepository, DistributionMapper distributionMapper, CepAttachmentsRepository cepAttachmentsRepository, JournalMapper journalMapper, JournalRepository journalRepository, HandingOverRepository handingOverRepository, CashLimitRepository cashLimitRepository) {
        this.masterClient = masterClient;
        this.organizerRepository = organizerRepository;
        this.organizerMapper = organizerMapper;
        this.calenderMapper = calenderMapper;
        this.calenderRepository = calenderRepository;
        this.courseMapper = courseMapper;
        this.courseRepository = courseRepository;
        this.requisitionMapper = requisitionMapper;
        this.requisitionRepository = requisitionRepository;
        this.feedbackMapper = feedbackMapper;
        this.feedbackRepository = feedbackRepository;
        this.transactionRepository = transactionRepository;
        this.masterCacheService = masterCacheService;
        this.notificationRepository = notificationRepository;
        this.signRoleAuthorityRepository = signRoleAuthorityRepository;
        this.sequenceRepository = sequenceRepository;
        this.evaluationRepository = evaluationRepository;
        this.eligibilityMapper = eligibilityMapper;
        this.eligibilityRepository = eligibilityRepository;
        this.courseTypeRepository = courseTypeRepository;
        this.loginRepository = loginRepository;
        this.cepRepository = cepRepository;
        this.cepMapper = cepMapper;
        this.distributionRepository = distributionRepository;
        this.distributionMapper = distributionMapper;
        this.cepAttachmentsRepository = cepAttachmentsRepository;
        this.journalMapper = journalMapper;
        this.journalRepository = journalRepository;
        this.handingOverRepository = handingOverRepository;
        this.cashLimitRepository = cashLimitRepository;
    }

    @Transactional(readOnly = true)
    public List<OrganizerDTO> getAllAgencies() {

        List<Organizer> list = organizerRepository.findAllByIsActive(1);

        list = list.stream()
                .sorted(
                        Comparator.comparing(
                                Organizer::getOrganizer,
                                Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)
                        )
                )
                .toList();

        return list.stream()
                .map(organizerMapper::toDto)
                .collect(Collectors.toCollection(LinkedList::new));
    }

    @Transactional
    public CalendarDTO addCalenderData(CalendarDTO dto, String username) throws IOException {
        log.info("Request to save calender for year {} by {}", dto.getYear(), username);
        Calendar calender = calenderMapper.toEntity(dto);
        calender.setCreatedBy(username);
        calender.setCreatedDate(LocalDateTime.now());
        calender.setIsActive(1);

        OrganizerDTO organizerDTO = getOrganizerById(dto.getOrganizerId())
                .orElseThrow(() -> new NotFoundException("Organizer data not found"));

        Path fullpath = Paths.get(appStorage, "Calendar", dto.getYear(), organizerDTO.getOrganizer().trim());

        if (dto.getFile() != null && !dto.getFile().isEmpty()) {
            calender.setCalendarFileName(dto.getFile().getOriginalFilename());
            FileStorageUtil.saveFile(fullpath, dto.getFile().getOriginalFilename(), dto.getFile());
        }

        if (dto.getCoverFile() != null && !dto.getCoverFile().isEmpty()) {
            calender.setCoveringLetter(dto.getCoverFile().getOriginalFilename());
            FileStorageUtil.saveFile(fullpath, dto.getCoverFile().getOriginalFilename(), dto.getCoverFile());
        }

        calender = calenderRepository.save(calender);
        return calenderMapper.toDto(calender);
    }

    @Transactional(readOnly = true)
    public List<CalendarDTO> getCalenderList(String year, String username) {
        log.info("Calender list fetched by {}", username);
        List<Organizer> agencies = organizerRepository.findAllByIsActive(1);
        List<Calendar> calenderList = calenderRepository.findAllByYearAndIsActive(year, 1);

        Map<Long, Organizer> agencyMap = agencies.stream()
                .collect(Collectors.toMap(Organizer::getOrganizerId, Function.identity()));

        List<CalendarDTO> dtoList = calenderMapper.toDto(calenderList);

        dtoList.forEach(dto -> {
            Organizer organizer = agencyMap.get(dto.getOrganizerId());
            if (organizer != null) {
                dto.setOrganizer(organizer.getOrganizer());
            }
        });
        return dtoList;
    }

    public Optional<CalendarDTO> getCalendarById(Long id, String username) {
        return calenderRepository.findById(id).map(calenderMapper::toDto);
    }

    @CacheEvict(value = "courseCache", allEntries = true)
    @Transactional
    public CourseDTO addCourseData(@Valid CourseDTO dto, String username) {
        log.info("Request to add course {} by {}", dto.getCourseName(), username);
        Course course = courseMapper.toEntity(dto);
        course.setCreatedBy(username);
        course.setCreatedDate(LocalDateTime.now());
        course.setIsActive(1);
        course = courseRepository.save(course);

        Organizer organizer = organizerRepository.findById(course.getOrganizerId())
                .orElseThrow(() -> new NotFoundException("Organizer data not found"));

        CourseDTO courseDTO = courseMapper.toDto(course);
        courseDTO.setOrganizer(organizer.getOrganizer());

        return courseDTO;
    }

    @Transactional(readOnly = true)
    public List<CourseDTO> getCourseList(Long orgId, String username) {
        log.info("Course list fetched for organizer id {} by {}", orgId, username);

        if (orgId == null) {
            return List.of();
        }
        List<Course> courseList;
        if (orgId > 0) {
            courseList = courseRepository.findAllByOrganizerIdAndIsActive(orgId, 1);
        } else {
            courseList = courseRepository.findAllByIsActive(1);
        }
        List<Eligibility> eligibilityList = eligibilityRepository.findAllByIsActive(1);

        courseList = courseList.stream()
                .sorted(Comparator.comparing(Course::getCreatedDate).reversed())
                .toList();

        Map<Long, Organizer> organizerMap = masterCacheService.getOrganizerMap();
        Map<Long, Eligibility> eligibilityMap = eligibilityList.stream()
                .collect(Collectors.toMap(Eligibility::getEligibilityId, Function.identity()));

        Map<Long, CourseType> courseTypeMap = masterCacheService.getCourseTypeMap();

        List<CourseDTO> dtoList = courseMapper.toDto(courseList);

        dtoList.forEach(dto -> {
            Organizer organizer = organizerMap.get(dto.getOrganizerId());
            Eligibility eligibility = eligibilityMap.get(dto.getEligibilityId());
            CourseType courseType = courseTypeMap.get(dto.getCourseTypeId());

            if (organizer != null) {
                dto.setOrganizer(organizer.getOrganizer());
            }
            if (eligibility != null) {
                dto.setEligibilityName(eligibility.getEligibilityName());
            }
            if (courseType != null) {
                dto.setCourseType(courseType.getCourseType());
            }
        });
        return dtoList;
    }

    @Transactional
    public RequisitionDTO addRequisitionData(@Valid RequisitionDTO dto, String username) throws IOException {
        log.info("Request to add requisition for program {} by {}", dto.getCourseName(), username);

        Requisition requisition = requisitionMapper.toEntity(dto);

        var status = "N".equalsIgnoreCase(dto.getIsMandatory()) ? "AA" : "AP";
        requisition.setStatus(status);
        requisition.setIsConfirmed("N");
        requisition.setIsAttend("N");
        requisition.setCreatedBy(username);
        requisition.setCreatedDate(LocalDateTime.now());
        requisition.setIsActive(1);

        LocalDate fromDate = requisition.getFromDate();

        if (fromDate == null) {
            throw new IllegalArgumentException("From Date is required");
        }

        String fy = getFinancialYear(fromDate);

        RequisitionSequence sequence =
                sequenceRepository.findByFinancialYearForUpdate(fy)
                        .orElseGet(() -> {
                            RequisitionSequence newSeq = new RequisitionSequence();
                            newSeq.setFinancialYear(fy);
                            newSeq.setLastNumber(0L);
                            return newSeq;
                        });

        Long nextNumber = sequence.getLastNumber() + 1;
        sequence.setLastNumber(nextNumber);
        sequenceRepository.save(sequence);

        String requisitionNumber = "REQ/" + fy + "/" + String.format("%03d", nextNumber);
        requisition.setRequisitionNumber(requisitionNumber);

        Path fullpath = Paths.get(appStorage, "Requisition", requisitionNumber.replace("/", "_"));
        if (dto.getMultipartFileEcs() != null && !dto.getMultipartFileEcs().isEmpty()) {
            requisition.setFileEcs(dto.getMultipartFileEcs().getOriginalFilename());
            FileStorageUtil.saveFile(fullpath, dto.getMultipartFileEcs().getOriginalFilename(), dto.getMultipartFileEcs());
        }
        if (dto.getMultipartFileCheque() != null && !dto.getMultipartFileCheque().isEmpty()) {
            requisition.setFileCheque(dto.getMultipartFileCheque().getOriginalFilename());
            FileStorageUtil.saveFile(fullpath, dto.getMultipartFileCheque().getOriginalFilename(), dto.getMultipartFileCheque());
        }
        if (dto.getMultipartFilePan() != null && !dto.getMultipartFilePan().isEmpty()) {
            requisition.setFilePan(dto.getMultipartFilePan().getOriginalFilename());
            FileStorageUtil.saveFile(fullpath, dto.getMultipartFilePan().getOriginalFilename(), dto.getMultipartFilePan());
        }
        if (dto.getMultipartFileBrochure() != null && !dto.getMultipartFileBrochure().isEmpty()) {
            requisition.setFileBrochure(dto.getMultipartFileBrochure().getOriginalFilename());
            FileStorageUtil.saveFile(fullpath, dto.getMultipartFileBrochure().getOriginalFilename(), dto.getMultipartFileBrochure());
        }
        if (dto.getMultipartCommitteeApproval() != null && !dto.getMultipartCommitteeApproval().isEmpty()) {
            requisition.setFileCommitteeApproval(dto.getMultipartCommitteeApproval().getOriginalFilename());
            FileStorageUtil.saveFile(fullpath, dto.getMultipartCommitteeApproval().getOriginalFilename(), dto.getMultipartCommitteeApproval());
        }
        if (dto.getMultipartAcceptanceLetter() != null && !dto.getMultipartAcceptanceLetter().isEmpty()) {
            requisition.setFileAcceptanceLetter(dto.getMultipartAcceptanceLetter().getOriginalFilename());
            FileStorageUtil.saveFile(fullpath, dto.getMultipartAcceptanceLetter().getOriginalFilename(), dto.getMultipartAcceptanceLetter());
        }
        if (dto.getMultipartPaper() != null && !dto.getMultipartPaper().isEmpty()) {
            requisition.setFilePaper(dto.getMultipartPaper().getOriginalFilename());
            FileStorageUtil.saveFile(fullpath, dto.getMultipartPaper().getOriginalFilename(), dto.getMultipartPaper());
        }

        requisition = requisitionRepository.save(requisition);
        insertTransaction(requisition.getRequisitionId(), requisition.getInitiatingOfficer(), requisition.getInitiatingOfficer(), username, status, null);
        return requisitionMapper.toDto(requisition);
    }

    @Transactional(readOnly = true)
    public List<RequisitionDTO> getRequisitionList(
            Long empId,
            String roleName,
            LocalDate fromDate,
            LocalDate toDate,
            Long selectedEmployeeId,
            String username,
            String isMandatory) {

        log.info("Requisition list fetched for role {} by {}", roleName, username);

        List<EmployeeDTO> employeeList = masterClient.getEmployeeMasterList(xApiKey);

        Map<Long, EmployeeDTO> employeeMap = masterCacheService.getLongEmployeeDTOMap();
        Map<Long, Organizer> organizerMap = masterCacheService.getOrganizerMap();
        Map<Long, Course> courseMap = masterCacheService.getCourseMap();
        Map<Long, CourseType> courseTypeMap = masterCacheService.getCourseTypeMap();
        Map<String, Status> statusMap = masterCacheService.getStatusMap();

        List<Requisition> requisitions = fetchRequisitions(roleName, empId, employeeList);

        if (fromDate != null && toDate != null) {
            requisitions = requisitions.stream()
                    .filter(r -> r.getFromDate() != null
                            && !r.getFromDate().isBefore(fromDate)
                            && !r.getFromDate().isAfter(toDate))
                    .toList();
        }

        if (selectedEmployeeId != null && selectedEmployeeId > 0) {
            requisitions = requisitions.stream()
                    .filter(r -> Objects.equals(r.getInitiatingOfficer(), selectedEmployeeId))
                    .toList();
        }

        if (StringUtils.hasText(isMandatory)) {
            requisitions = requisitions.stream()
                    .filter(r -> isMandatory.equalsIgnoreCase(r.getIsMandatory()))
                    .toList();
        }

        List<RequisitionDTO> dtoList = requisitionMapper.toDto(requisitions);

        Map<Long, Journal> journalMap = journalRepository
                .findAllByIsActiveOrderByJournalIdDesc(1)
                .stream()
                .collect(Collectors.toMap(Journal::getJournalId, Function.identity()));

        dtoList.forEach(dto -> {

            EmployeeDTO employee = employeeMap.get(dto.getInitiatingOfficer());

            if (employee != null) {
                dto.setEmpNo(employee.getEmpNo());
                dto.setInitiatingOfficerName(CommonUtil.buildEmployeeName(employee, false));
                dto.setEmpDesigName(employee.getEmpDesigName());
                dto.setEmpDivCode(employee.getEmpDivCode());
                dto.setEmail(employee.getEmail());
                dto.setMobileNo(employee.getMobileNo());
                dto.setIsGroup(employee.getIsGroup());
            }

            Status status = statusMap.get(dto.getStatus());
            if (status != null) {
                dto.setStatusName(status.getStatusName());
                dto.setStatusColor(status.getColorCode());
            }

            Course course = courseMap.get(dto.getCourseId());
            if (course != null) {

                dto.setCourseName(course.getCourseName());
                dto.setCourseLevel(course.getCourseLevel());
                dto.setVenue(course.getVenue());
                dto.setOfflineRegistrationFee(course.getOfflineRegistrationFee());
                dto.setOnlineRegistrationFee(course.getOnlineRegistrationFee());

                Organizer organizer = organizerMap.get(course.getOrganizerId());

                if (organizer != null) {
                    dto.setOrganizer(organizer.getOrganizer());
                    dto.setOrganizerContactName(organizer.getContactName());
                    dto.setOrganizerPhoneNo(organizer.getPhoneNo());
                    dto.setOrganizerFaxNo(organizer.getFaxNo());
                    dto.setOrganizerEmail(organizer.getEmail());
                }

                CourseType courseType = courseTypeMap.get(course.getCourseTypeId());

                if (courseType != null) {
                    dto.setCourseType(courseType.getCourseType());
                }
            }

            if (dto.getJournalId() != null) {
                Journal journal = journalMap.get(dto.getJournalId());

                if (journal != null) {
                    dto.setTitleOfPaper(journal.getTitleOfPaper());
                }
            }
        });

        return dtoList;
    }

    private List<Requisition> fetchRequisitions(
            String roleName,
            Long empId,
            List<EmployeeDTO> employeeList) {

        Set<String> adminRoles = Set.of(
                "ROLE_ADMIN",
                "ROLE_AD_HRT",
                "ROLE_SA_HRT",
                "ROLE_DIRECTOR",
                "ROLE_CAG_DIV",
                "ROLE_TCG_DIV",
                "ROLE_SM_HRT"
        );

        if (adminRoles.contains(roleName)) {
            return requisitionRepository.findAllByIsActiveOrderByRequisitionIdDesc(1);
        }

        if ("ROLE_DH".equalsIgnoreCase(roleName)) {

            Optional<DivisionDTO> division = masterClient.getDivisionMaster(xApiKey)
                    .stream()
                    .filter(d -> Objects.equals(d.getDivisionHeadId(), empId))
                    .findFirst();

            if (division.isEmpty()) {
                return requisitionRepository
                        .findAllByInitiatingOfficerAndIsActiveOrderByRequisitionIdDesc(empId, 1);
            }

            Long divisionId = division.get().getDivisionId();

            List<Long> empIds = employeeList.stream()
                    .filter(e -> labCode != null && labCode.equalsIgnoreCase(e.getLabCode()))
                    .filter(e -> Objects.equals(e.getDivisionId(), divisionId))
                    .map(EmployeeDTO::getEmpId)
                    .collect(Collectors.toCollection(ArrayList::new));

            // Include DH's own requisitions
            empIds.add(empId);

            return requisitionRepository
                    .findAllByInitiatingOfficerInAndIsActiveOrderByRequisitionIdDesc(
                            empIds, 1);
        }

        if ("ROLE_GH".equalsIgnoreCase(roleName)) {

            if ("CAIR".equalsIgnoreCase(labCode)) {

                Optional<DivisionDTO> division = masterClient.getDivisionMaster(xApiKey)
                        .stream()
                        .filter(d -> Objects.equals(d.getDivisionHeadId(), empId))
                        .findFirst();

                if (division.isEmpty()) {
                    return requisitionRepository
                            .findAllByInitiatingOfficerAndIsActiveOrderByRequisitionIdDesc(empId, 1);
                }

                Long divisionId = division.get().getDivisionId();

                List<Long> empIds = employeeList.stream()
                        .filter(e -> labCode != null && labCode.equalsIgnoreCase(e.getLabCode()))
                        .filter(e -> Objects.equals(e.getDivisionId(), divisionId))
                        .map(EmployeeDTO::getEmpId)
                        .collect(Collectors.toCollection(ArrayList::new));

                // Include GH's own requisitions
                empIds.add(empId);

                return requisitionRepository
                        .findAllByInitiatingOfficerInAndIsActiveOrderByRequisitionIdDesc(
                                empIds, 1);
            } else {

                Optional<DivisionGroupDTO> group = masterClient
                        .getDivisionGroupMasterList(xApiKey)
                        .stream()
                        .filter(g -> Objects.equals(g.getGroupHeadId(), empId))
                        .findFirst();

                if (group.isEmpty()) {
                    return Collections.emptyList();
                }

                Set<Long> divisionIds = masterClient.getDivisionMaster(xApiKey)
                        .stream()
                        .filter(d -> Objects.equals(d.getGroupId(), group.get().getGroupId()))
                        .map(DivisionDTO::getDivisionId)
                        .collect(Collectors.toSet());

                List<Long> empIds = employeeList.stream()
                        .filter(e -> divisionIds.contains(e.getDivisionId()))
                        .map(EmployeeDTO::getEmpId)
                        .collect(Collectors.toCollection(ArrayList::new));

                // Include GH's own requisitions
                empIds.add(empId);

                return requisitionRepository
                        .findAllByInitiatingOfficerInAndIsActiveOrderByRequisitionIdDesc(empIds, 1);
            }
        }

        return requisitionRepository
                .findAllByInitiatingOfficerAndIsActiveOrderByRequisitionIdDesc(empId, 1);
    }

    @Transactional
    public RequisitionDTO getRequisitionById(Long id, String username) {
        log.info("Request to fetch Requisition data for id {} by {}", id, username);
        if (id == null) {
            throw new NotFoundException("Requisition id cannot be null");
        }
        Map<Long, Organizer> organizerMap = masterCacheService.getOrganizerMap();
        Map<Long, Course> courseMap = masterCacheService.getCourseMap();
        Map<Long, CourseType> courseTypeMap = masterCacheService.getCourseTypeMap();

        Requisition requisition = requisitionRepository.findById(id).orElseThrow(() -> new NotFoundException("Requisition not found"));

        Course course = courseMap.get(requisition.getCourseId());
        Organizer org = organizerMap.get(course.getOrganizerId());
        CourseType courseType = courseTypeMap.get(course.getCourseTypeId());


        RequisitionDTO requisitionDTO = requisitionMapper.toDto(requisition);
        requisitionDTO.setOrganizer(org.getOrganizer());
        requisitionDTO.setCourseType(courseType.getCourseType());
        requisitionDTO.setOrganizerId(org.getOrganizerId());
        requisitionDTO.setCourseName(course.getCourseName());
        requisitionDTO.setVenue(course.getVenue());
        requisitionDTO.setOfflineRegistrationFee(course.getOfflineRegistrationFee());
        requisitionDTO.setOnlineRegistrationFee(course.getOnlineRegistrationFee());
        return requisitionDTO;
    }

    @Transactional
    public Optional<RequisitionDTO> updateRequisitionData(@Valid RequisitionDTO dto, String username) {

        log.info("Request to update requisition for id {} by {}", dto.getRequisitionId(), username);

        Path fullpath = Paths.get(appStorage, "Requisition", dto.getRequisitionNumber().replace("/", "_"));

        return requisitionRepository
                .findById(dto.getRequisitionId())
                .map(existingReq -> {
                    existingReq.setModifiedBy(username);
                    existingReq.setModifiedDate(LocalDateTime.now());

                    // ECS
                    updateFile(dto.getMultipartFileEcs(), existingReq.getFileEcs(), fullpath, existingReq::setFileEcs);
                    // Cheque
                    updateFile(dto.getMultipartFileCheque(), existingReq.getFileCheque(), fullpath, existingReq::setFileCheque);
                    // PAN
                    updateFile(dto.getMultipartFilePan(), existingReq.getFilePan(), fullpath, existingReq::setFilePan);
                    // Brochure
                    updateFile(dto.getMultipartFileBrochure(), existingReq.getFileBrochure(), fullpath, existingReq::setFileBrochure);
                    // Committee Approval Letter
                    updateFile(dto.getMultipartCommitteeApproval(), existingReq.getFileCommitteeApproval(), fullpath, existingReq::setFileCommitteeApproval);
                    // Paper Acceptance Letter
                    updateFile(dto.getMultipartAcceptanceLetter(), existingReq.getFileAcceptanceLetter(), fullpath, existingReq::setFileAcceptanceLetter);
                    // Paper
                    updateFile(dto.getMultipartPaper(), existingReq.getFilePaper(), fullpath, existingReq::setFilePaper);

                    requisitionMapper.partialUpdate(existingReq, dto);
                    return existingReq;
                })
                .map(requisitionRepository::save)
                .map(requisitionMapper::toDto);
    }

    private void updateFile(MultipartFile multipartFile,
                            String existingFileName,
                            Path fullPath,
                            Consumer<String> setFileName) {

        if (multipartFile != null && !multipartFile.isEmpty()) {
            // Delete old file if exists
            if (existingFileName != null) {
                Path oldFilePath = fullPath.resolve(existingFileName);
                try {
                    if (Files.exists(oldFilePath)) {
                        Files.delete(oldFilePath);
                        log.info("Deleted old file: {}", oldFilePath);
                    }
                } catch (Exception ex) {
                    log.warn("Failed to delete old file: {}", oldFilePath, ex);
                }
            }
            // Save new file
            String newFileName = multipartFile.getOriginalFilename();
            try {
                FileStorageUtil.saveFile(fullPath, newFileName, multipartFile);
                setFileName.accept(newFileName);
            } catch (IOException e) {
                throw new RuntimeException("Failed to store file", e);
            }
        }
    }


    public FeedbackDTO requisitionFeedback(@Valid FeedbackDTO dto, String username) throws IOException {
        log.info("Request to requisition feedback requisitionId {} by {}", dto.getRequisitionId(), username);
        Feedback feedback = feedbackMapper.toEntity(dto);
        feedback.setIsAccepted("N");
        feedback.setCreatedBy(username);
        feedback.setCreatedDate(LocalDateTime.now());
        feedback.setIsActive(1);

        Requisition requisition = requisitionRepository.findById(dto.getRequisitionId())
                .orElseThrow(() -> new NotFoundException("Requisition data not found"));

        if ("N".equalsIgnoreCase(requisition.getIsAttend())) {
            requisition.setIsAttend("Y");
            requisition.setModifiedBy(username);
            requisition.setModifiedDate(LocalDateTime.now());
            requisitionRepository.save(requisition);
        }

        Path fullpath = Paths.get(appStorage, "Requisition",
                requisition.getRequisitionNumber().replace("/", "_"), "Feedback");

        if (dto.getCertificateFile() != null && !dto.getCertificateFile().isEmpty()) {
            feedback.setCertificate(dto.getCertificateFile().getOriginalFilename());
            FileStorageUtil.saveFile(fullpath, dto.getCertificateFile().getOriginalFilename(), dto.getCertificateFile());
        }

        if (dto.getInvoiceFile() != null && !dto.getInvoiceFile().isEmpty()) {
            feedback.setInvoice(dto.getInvoiceFile().getOriginalFilename());
            FileStorageUtil.saveFile(fullpath, dto.getInvoiceFile().getOriginalFilename(), dto.getInvoiceFile());
        }

        feedback = feedbackRepository.save(feedback);
        return feedbackMapper.toDto(feedback);
    }

    public List<FeedbackDTO> getFeedbackList(Long empId, String roleName, String username) {
        log.info("Feedback list fetched by {}", username);

        List<Feedback> feedbackList;
        List<EmployeeDTO> employeeList = masterClient.getEmployeeMasterList(xApiKey);

        if (Arrays.asList("ROLE_ADMIN", "ROLE_AD_HRT", "ROLE_SA_HRT", "ROLE_DIRECTOR",
                "ROLE_CAG_DIV", "ROLE_TCG_DIV", "ROLE_SM_HRT").contains(roleName) && empId == 0) {

            feedbackList = feedbackRepository.findByIsActiveOrderByFeedbackIdDesc(1);

        } else if ("ROLE_DH".equalsIgnoreCase(roleName) || "ROLE_GH".equalsIgnoreCase(roleName)) {

            List<DivisionDTO> divisionList = masterClient.getDivisionMaster(xApiKey);

            Optional<DivisionDTO> divisionOpt = divisionList.stream()
                    .filter(d -> Objects.equals(d.getDivisionHeadId(), empId))
                    .findFirst();

            if (divisionOpt.isPresent()) {

                Long divisionId = divisionOpt.get().getDivisionId();
                List<Long> empIds = employeeList.stream()
                        .filter(e -> labCode != null && labCode.equalsIgnoreCase(e.getLabCode()))
                        .filter(emp -> Objects.equals(emp.getDivisionId(), divisionId))
                        .map(EmployeeDTO::getEmpId)
                        .collect(Collectors.toList());

                feedbackList = feedbackRepository
                        .findAllByParticipantIdInAndIsActiveOrderByFeedbackIdDesc(empIds, 1);

            } else {
                feedbackList = new ArrayList<>();
            }

        } else {
            feedbackList = feedbackRepository
                    .findAllByParticipantIdAndIsActiveOrderByFeedbackIdDesc(empId, 1);
        }

        if (feedbackList == null || feedbackList.isEmpty()) {
            return Collections.emptyList();
        }

        List<FeedbackDTO> feedbbackdto = feedbackMapper.toDto(feedbackList);

        if (feedbbackdto == null || feedbbackdto.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, EmployeeDTO> employeeMap = employeeList != null
                ? employeeList.stream()
                .filter(e -> labCode != null && labCode.equalsIgnoreCase(e.getLabCode()))
                .collect(Collectors.toMap(EmployeeDTO::getEmpId, emp -> emp))
                : Collections.emptyMap();

        feedbbackdto.forEach(d -> {

            if (d == null) return;

            EmployeeDTO employeeDTO = employeeMap.get(d.getParticipantId());

            if (employeeDTO != null) {
                d.setParticipantName(CommonUtil.buildEmployeeName(employeeDTO, true));
                d.setDivisionName(employeeDTO.getEmpDivCode());
                d.setIsGroup(employeeDTO.getIsGroup());
            }

            RequisitionDTO requisitionDto = null;

            if (d.getRequisitionId() != null) {
                requisitionDto = getRequisitionById(d.getRequisitionId(), username);
            }

            if (requisitionDto != null) {
                d.setRequisitionNumber(requisitionDto.getRequisitionNumber());
                d.setCourseName(requisitionDto.getCourseName());
                d.setOrganizer(requisitionDto.getOrganizer());
                d.setFromDate(requisitionDto.getFromDate());
                d.setToDate(requisitionDto.getToDate());
                d.setProgramDuration(requisitionDto.getDuration());
            }

        });

        return feedbbackdto;
    }

    @CacheEvict(value = "courseCache", allEntries = true)
    @Transactional
    public Optional<CourseDTO> editCourseData(@Valid CourseDTO dto, String username) {
        log.info("Request to edit program id {} by {}", dto.getCourseId(), username);

        return courseRepository
                .findById(dto.getCourseId())
                .map(existingCourse -> {
                    existingCourse.setModifiedBy(username);
                    existingCourse.setModifiedDate(LocalDateTime.now());
                    courseMapper.partialUpdate(existingCourse, dto);
                    return existingCourse;
                })
                .map(courseRepository::save)
                .map(courseMapper::toDto);
    }

    @CacheEvict(value = "organizerCache", allEntries = true)
    public OrganizerDTO addOrganizer(@Valid OrganizerDTO dto, String username) {
        log.info("Request to add organizer {} by {}", dto.getOrganizer(), username);
        Organizer organizer = organizerMapper.toEntity(dto);
        organizer.setCreatedBy(username);
        organizer.setCreatedDate(LocalDateTime.now());
        organizer.setIsActive(1);
        organizer = organizerRepository.save(organizer);

        return organizerMapper.toDto(organizer);
    }

    @CacheEvict(value = "organizerCache", allEntries = true)
    public Optional<OrganizerDTO> editOrganizer(@Valid OrganizerDTO dto, String username) {
        log.info("Request to edit organizer {} by {}", dto.getOrganizer(), username);

        return organizerRepository
                .findById(dto.getOrganizerId())
                .map(organizer -> {
                    organizer.setModifiedBy(username);
                    organizer.setModifiedDate(LocalDateTime.now());
                    organizerMapper.partialUpdate(organizer, dto);
                    return organizer;
                })
                .map(organizerRepository::save)
                .map(organizerMapper::toDto);
    }

    @CacheEvict(value = "notificationList", allEntries = true)
    @Transactional
    public RequisitionDTO forwardRequisition(@Valid RequisitionDTO dto, String username) {
        log.info("Request to froward requisition for id {} ", dto.getRequisitionId());

        if (dto.getRequisitionId() == null) {
            throw new NotFoundException("Requisition Id can not be null");
        }

        Requisition requisition = requisitionRepository.findById(dto.getRequisitionId())
                .orElseThrow(() -> new NotFoundException("Requisition not found"));


        Map<Long, EmployeeDTO> employeeMap = masterCacheService.getLongEmployeeDTOMap();

        EmployeeDTO employeeDTO = employeeMap.get(requisition.getInitiatingOfficer());

        String message = getNotificationMsg(requisition.getRequisitionNumber(), employeeDTO, "Forward by");

        if ("RS".equalsIgnoreCase(requisition.getStatus())) {
            requisition.setStatus("SF");

            List<SignRoleAuthorityDTO> authorityDTOList = signRoleAuthorityRepository.findBySignAuthRole("SA-HRT");
            if (authorityDTOList.isEmpty()) {
                throw new NotFoundException("In SignRoleAuthority SA-HRT role not found");
            }

            for (SignRoleAuthorityDTO authorityDTO : authorityDTOList) {
                insertTransaction(dto.getRequisitionId(), dto.getActionBy(), authorityDTO.getEmpId(), username, "SF", null);
                insertNotification(dto.getActionBy(), authorityDTO.getEmpId(), "req-approval", message, username);
            }
        } else {
            requisition.setStatus("AF");
            DivisionDTO divisionDTO = Optional.of(employeeDTO)
                    .map(EmployeeDTO::getDivisionId)
                    .flatMap(id -> Optional.ofNullable(masterClient.getDivisionMaster(xApiKey))
                            .orElse(Collections.emptyList())
                            .stream()
                            .filter(d -> id.equals(d.getDivisionId()))
                            .findFirst())
                    .orElseThrow(() -> new NotFoundException("Division data not found"));

            insertTransaction(dto.getRequisitionId(), dto.getActionBy(), divisionDTO.getDivisionHeadId(), username, "AF", null);
            insertNotification(dto.getActionBy(), divisionDTO.getDivisionHeadId(), "req-approval", message, username);
        }
        requisition.setModifiedBy(username);
        requisition.setModifiedDate(LocalDateTime.now());
        return requisitionMapper.toDto(requisition);
    }

    @CacheEvict(value = "notificationList", allEntries = true)
    @Transactional
    public RequisitionDTO recommendRequisition(@Valid RequisitionDTO dto, String username) {

        log.info("Request to recommend requisition for id {}", dto.getRequisitionId());

        if (dto.getRequisitionId() == null) {
            throw new NotFoundException("Requisition Id cannot be null");
        }

        Requisition requisition = requisitionRepository.findById(dto.getRequisitionId())
                .orElseThrow(() -> new NotFoundException("Requisition not found"));

        CashLimit cashLimit = cashLimitRepository
                .findTopByIsActiveOrderByCashLimitIdDesc(1)
                .orElseThrow(() -> new NotFoundException("Cash limit is not configured."));

        BigDecimal registrationFee = Optional.ofNullable(requisition.getRegistrationFee())
                .orElse(BigDecimal.ZERO);

        BigDecimal limit = Optional.ofNullable(cashLimit)
                .map(CashLimit::getCashLimit)
                .orElse(BigDecimal.ZERO);

        String status = requisition.getStatus();
        EmployeeDTO initiator = masterCacheService.getLongEmployeeDTOMap().get(requisition.getInitiatingOfficer());

        if ("AS".equalsIgnoreCase(status)) {

            if (registrationFee.compareTo(limit) >= 0) {
                requisition.setStatus("CA");
                forwardToRole(requisition, dto.getActionBy(), username, "AD-HRT", "CA");
            } else {
                requisition.setStatus("AV");
                insertTransaction(dto.getRequisitionId(), dto.getActionBy(), dto.getActionBy(), username, "AV", null);
            }

        } else if ("CA".equalsIgnoreCase(status)) {

            requisition.setStatus("AV");
            insertTransaction(dto.getRequisitionId(), dto.getActionBy(), dto.getActionBy(), username, "AV", null);

        } else if ("AF".equalsIgnoreCase(status)) {

            // Change AR to AG only for group users
            String isGroup = initiator.getIsGroup();

            if ("Y".equalsIgnoreCase(isGroup)) {

                requisition.setStatus("AG");
                forwardToRole(requisition, dto.getActionBy(), username, "SA-HRT", "AG");

            } else if ("B".equalsIgnoreCase(isGroup)) {

                requisition.setStatus("AG");

                // Get Division Master
                List<DivisionDTO> divisionList = masterClient.getDivisionMaster(xApiKey);

                DivisionDTO division = divisionList.stream()
                        .filter(Objects::nonNull)
                        .filter(e -> Objects.equals(e.getDivisionId(), initiator.getDivisionId()))
                        .findFirst()
                        .orElseThrow(() -> new NotFoundException("Division not found for divisionId: " + initiator.getDivisionId()));

                if (division.getGroupId() == null) {
                    throw new BadRequestException("Group is not mapped to division: " + initiator.getDivisionId());
                }

                // Get Division Group Master
                List<DivisionGroupDTO> groupList = masterClient.getDivisionGroupMasterList(xApiKey);

                DivisionGroupDTO group = groupList.stream()
                        .filter(Objects::nonNull)
                        .filter(e -> Objects.equals(e.getGroupId(), division.getGroupId()))
                        .findFirst()
                        .orElseThrow(() -> new NotFoundException("Division group not found for groupId: " + division.getGroupId()));

                Long groupHeadId = group.getGroupHeadId();

                if (groupHeadId == null) {
                    throw new BadRequestException("Group head is not configured for groupId: " + division.getGroupId());
                }

                EmployeeDTO employee = masterCacheService.getLongEmployeeDTOMap().get(dto.getActionBy());
                String message = getNotificationMsg(requisition.getRequisitionNumber(), employee, "Forward by");

                insertTransaction(dto.getRequisitionId(), dto.getActionBy(), groupHeadId, username, "AG", null);
                insertNotification(dto.getActionBy(), groupHeadId, "req-approval", message, username);

            } else {

                requisition.setStatus("AR");
                forwardToRole(requisition, dto.getActionBy(), username, "SA-HRT", "AR");
            }

        } else if ("B".equalsIgnoreCase(initiator.getIsGroup()) && "AG".equalsIgnoreCase(status)) {

            requisition.setStatus("AR");
            forwardToRole(requisition, dto.getActionBy(), username, "SA-HRT", "AR");

        } else if ("AR".equalsIgnoreCase(status) || "AG".equalsIgnoreCase(status) || "SF".equalsIgnoreCase(status)) {

            requisition.setStatus("AS");
            String role = registrationFee.compareTo(limit) >= 0 ? "CAG-Div" : "AD-HRT";
            forwardToRole(requisition, dto.getActionBy(), username, role, "AS");
        }

        requisition.setModifiedBy(username);
        requisition.setModifiedDate(LocalDateTime.now());

        return requisitionMapper.toDto(requisitionRepository.save(requisition));
    }

    private void forwardToRole(Requisition requisition, Long actionBy, String username, String role, String transactionStatus) {

        List<SignRoleAuthorityDTO> authorities = signRoleAuthorityRepository.findBySignAuthRole(role);

        if (authorities.isEmpty()) {
            throw new NotFoundException("SignRoleAuthority " + role + " role not found");
        }

        EmployeeDTO employee = masterCacheService.getLongEmployeeDTOMap().get(actionBy);
        String message = getNotificationMsg(requisition.getRequisitionNumber(), employee, "Forward by");

        for (SignRoleAuthorityDTO authority : authorities) {
            insertTransaction(requisition.getRequisitionId(), actionBy, authority.getEmpId(), username, transactionStatus, null);
            insertNotification(actionBy, authority.getEmpId(), "req-approval", message, username);
        }
    }


    private static String getNotificationMsg(String requisitionNumber, EmployeeDTO employeeDTO, String messageName) {
        String prefix = employeeDTO.getTitle() != null && !employeeDTO.getTitle().trim().isEmpty()
                ? employeeDTO.getTitle()
                : (employeeDTO.getSalutation() != null ? employeeDTO.getSalutation() : "");

        return String.format(
                "Requisition no " + requisitionNumber + " " + messageName + " " + "%s%s%s",
                prefix.isEmpty() ? "" : prefix + " ",
                employeeDTO.getEmpName() != null ? employeeDTO.getEmpName() : "",
                employeeDTO.getEmpDesigName() != null ? ", " + employeeDTO.getEmpDesigName() : ""
        );
    }

    @Transactional(readOnly = true)
    public RequisitionDTO getRequisitionPrint(Long id, String username) {

        log.info("Request to fetch Requisition print data for id {} by {}", id, username);

        if (id == null) {
            throw new NotFoundException("Requisition id cannot be null");
        }

        // Fetch requisition
        Requisition requisition = requisitionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Requisition not found"));

        RequisitionDTO dto = requisitionMapper.toDto(requisition);

        Map<Long, Organizer> organizerMap = masterCacheService.getOrganizerMap();
        Map<Long, Course> courseMap = masterCacheService.getCourseMap();
        Map<Long, EmployeeDTO> employeeMap = masterCacheService.getLongEmployeeDTOMap();

        // Fetch latest transactions
        List<RequisitionTransaction> transactions =
                transactionRepository.findByRequisitionIdAndIsActiveOrderByActionDateDesc(
                        requisition.getRequisitionId(), 1);

        Map<String, RequisitionTransaction> transactionMap =
                Optional.ofNullable(transactions)
                        .orElse(Collections.emptyList())
                        .stream()
                        .collect(Collectors.toMap(
                                RequisitionTransaction::getStatusCode,
                                Function.identity(),
                                (existing, replacement) -> existing
                        ));

        // =========================
        // Course & Organizer
        // =========================
        Course course = courseMap.get(requisition.getCourseId());

        if (course != null) {
            dto.setCourseName(course.getCourseName());
            dto.setVenue(course.getVenue());
            dto.setOfflineRegistrationFee(course.getOfflineRegistrationFee());
            dto.setOnlineRegistrationFee(course.getOnlineRegistrationFee());

            Organizer organizer = organizerMap.get(course.getOrganizerId());
            if (organizer != null) {
                dto.setOrganizerId(organizer.getOrganizerId());
                dto.setOrganizer(organizer.getOrganizer());
                dto.setOrganizerContactName(organizer.getContactName());
                dto.setOrganizerPhoneNo(organizer.getPhoneNo());
                dto.setOrganizerFaxNo(organizer.getFaxNo());
                dto.setOrganizerEmail(organizer.getEmail());
            }
        }

        // =========================
        // Initiating Officer
        // =========================
        EmployeeDTO initiator = employeeMap.get(requisition.getInitiatingOfficer());

        if (initiator != null) {
            dto.setEmpNo(initiator.getEmpNo());
            dto.setInitiatingOfficerName(
                    CommonUtil.buildEmployeeName(initiator, false)
            );
            dto.setEmpDesigName(initiator.getEmpDesigName());
            dto.setEmpDivCode(initiator.getEmpDivCode());
            dto.setEmail(initiator.getEmail());
            dto.setMobileNo(initiator.getMobileNo());
        }

        // =========================
        // Verified & Approved
        // =========================
        RequisitionTransaction forwardTxn = transactionMap.get("AF");
        RequisitionTransaction verifyTxn = transactionMap.get("AR");
        RequisitionTransaction approveTxn = transactionMap.get("AV");

        EmployeeDTO verified = null;
        EmployeeDTO approved = null;

        if (forwardTxn != null) dto.setForwardDate(forwardTxn.getActionDate());

        if (verifyTxn != null && verifyTxn.getActionBy() != null) {
            verified = employeeMap.get(verifyTxn.getActionBy());
            dto.setVerifiedBy(verifyTxn.getActionBy());
            dto.setVerifiedDate(verifyTxn.getActionDate());
        }

        if (approveTxn != null && approveTxn.getActionBy() != null) {
            approved = employeeMap.get(approveTxn.getActionBy());
            dto.setApprovedBy(approveTxn.getActionBy());
            dto.setApprovedDate(approveTxn.getActionDate());
        }

        if (verified != null) {
            dto.setVerifiedOfficerName(
                    CommonUtil.buildEmployeeName(verified, true)
            );
        }

        if (approved != null) {
            dto.setApprovedOfficerName(
                    CommonUtil.buildEmployeeName(approved, true)
            );
        }

        return dto;
    }


    @Transactional(readOnly = true)
    public List<RequisitionDTO> getRequisitionApprovalList(Long empId, String username) {

        log.info("Request to fetch Requisition approval data for empId {} by {}", empId, username);

        if (empId == null) {
            throw new NotFoundException("Employee id cannot be null");
        }

        List<String> statusCodes = List.of("AF", "SF", "AR", "AS", "CA", "FA", "AG");

        Set<Long> fromEmpIds = handingOverRepository.findByToEmpIdAndToDateGreaterThanEqualAndIsActive(empId, LocalDate.now(), 1)
                .stream().map(HandingOver::getFromEmpId).filter(Objects::nonNull).collect(Collectors.toSet());

        fromEmpIds.add(empId);

        List<Long> empIds = fromEmpIds.stream().toList();

        List<Requisition> requisitionList = requisitionRepository.findApprovalList(empIds, statusCodes);

        if (requisitionList.isEmpty()) {
            return Collections.emptyList();
        }

        List<RequisitionDTO> dtoList = requisitionMapper.toDto(requisitionList);
        // Fetch master data

        Map<String, Status> statusMap = masterCacheService.getStatusMap();
        Map<Long, Organizer> organizerMap = masterCacheService.getOrganizerMap();
        Map<Long, Course> courseMap = masterCacheService.getCourseMap();
        Map<Long, EmployeeDTO> employeeMap = masterCacheService.getLongEmployeeDTOMap();

        List<RequisitionTransaction> transactions =
                transactionRepository.findAllByActionToInAndStatusCodeInAndIsActive(empIds, statusCodes, 1);

        // Keep latest transaction per requisition
        Map<Long, RequisitionTransaction> transactionMap =
                transactions.stream()
                        .collect(Collectors.toMap(
                                RequisitionTransaction::getRequisitionId,
                                Function.identity(),
                                (existing, replacement) ->
                                        existing.getActionDate().isAfter(replacement.getActionDate())
                                                ? existing
                                                : replacement
                        ));


        for (RequisitionDTO dto : dtoList) {

            EmployeeDTO initiator = employeeMap.get(dto.getInitiatingOfficer());

            if (initiator != null) {
                dto.setEmpNo(initiator.getEmpNo());
                dto.setInitiatingOfficerName(
                        CommonUtil.buildEmployeeName(initiator, false)
                );
                dto.setEmpDesigName(initiator.getEmpDesigName());
                dto.setIsGroup(initiator.getIsGroup());
            }

            // Course + Organizer
            Course course = courseMap.get(dto.getCourseId());
            if (course != null) {

                dto.setCourseName(course.getCourseName());
                dto.setVenue(course.getVenue());
                dto.setOfflineRegistrationFee(course.getOfflineRegistrationFee());
                dto.setOnlineRegistrationFee(course.getOnlineRegistrationFee());

                Organizer organizer = organizerMap.get(course.getOrganizerId());
                if (organizer != null) {
                    dto.setOrganizerId(organizer.getOrganizerId());
                    dto.setOrganizer(organizer.getOrganizer());
                    dto.setOrganizerContactName(organizer.getContactName());
                    dto.setOrganizerPhoneNo(organizer.getPhoneNo());
                    dto.setOrganizerFaxNo(organizer.getFaxNo());
                    dto.setOrganizerEmail(organizer.getEmail());
                }
            }

            Status status = statusMap.get(dto.getStatus());
            if (status != null) {
                dto.setStatusName(status.getStatusName());
                dto.setStatusColor(status.getColorCode());
            }

            // Transaction Info
            RequisitionTransaction txn = transactionMap.get(dto.getRequisitionId());

            if (txn != null) {
                dto.setForwardDate(txn.getActionDate());
                dto.setActionTo(txn.getActionTo());
                if (txn.getActionBy() != null) {
                    EmployeeDTO forwarded = employeeMap.get(txn.getActionBy());
                    if (forwarded != null) {
                        dto.setForwardByName(CommonUtil.buildEmployeeName(forwarded, true));
                    }
                }
            }
        }

        return dtoList;
    }

    @Transactional(readOnly = true)
    public List<RequisitionTransactionDTO> getRequisitionTransaction(Long reqId, String username) {
        log.info("Request to fetch Requisition transaction data for requisitionId {} by {}", reqId, username);
        List<RequisitionTransaction> transactionList = transactionRepository.findFilteredTransactions(reqId);

        Map<String, Status> statusMap = masterCacheService.getStatusMap();
        Map<Long, EmployeeDTO> employeeMap = masterCacheService.getLongEmployeeDTOMap();

        return transactionList.stream().map(data -> {

            Status status = statusMap.get(data.getStatusCode());

            EmployeeDTO employeeBy = employeeMap.get(data.getActionBy());
            EmployeeDTO employeeTo = employeeMap.get(data.getActionTo());

            RequisitionTransactionDTO dto = new RequisitionTransactionDTO();

            dto.setTransactionId(data.getTransactionId());
            dto.setRequisitionId(data.getRequisitionId());
            dto.setActionDate(data.getActionDate());
            dto.setForwardBy(data.getActionBy());
            dto.setForwardByName(CommonUtil.buildEmployeeName(employeeBy, true));
            dto.setForwardTo(data.getActionTo());
            dto.setForwardToName(CommonUtil.buildEmployeeName(employeeTo, true));
            dto.setStatusCode(data.getStatusCode());
            dto.setStatusDetail(status != null ? status.getStatusName() : "");
            dto.setColorCode(status != null ? status.getColorCode() : "");
            dto.setRemarks(data.getRemarks());
            return dto;
        }).toList();
    }

    public static String getFinancialYear(LocalDate date) {

        int year = (date.getMonthValue() >= 4)
                ? date.getYear()
                : date.getYear() - 1;

        return year + "-" + (year + 1);
    }

    @Transactional
    public EvaluationRequestDTO addEvaluation(EvaluationRequestDTO dto, String username) {
        log.info("Request to add evaluation for empId {} by {} ", dto.getInitiator(), username);

        if (dto.getInitiator() == null) {
            throw new NotFoundException("Initiator can not be null");
        }
        Long initiatorId = dto.getInitiator();
        EvaluationDTO evaluationDTO = dto.getEvaluationData();
        Evaluation evaluation = new Evaluation();
        evaluation.setRequisitionId(evaluationDTO.getRequisitionId());
        evaluation.setTraineeId(initiatorId);
        evaluation.setImpact(evaluationDTO.getImpact());
        evaluation.setCreatedBy(username);
        evaluation.setCreatedDate(LocalDateTime.now());
        evaluation.setIsActive(1);

        evaluationRepository.save(evaluation);

        return dto;
    }

    @Transactional(readOnly = true)
    public List<EvaluationRequestDTO> getEvaluationList(LocalDate fromDate, LocalDate toDate, String username) {
        log.info("Request to fetch evaluation list by {}", username);

        List<EvaluationDTO> list = evaluationRepository.findEvaluationData(fromDate, toDate);

        Map<Long, EmployeeDTO> employeeMap = masterCacheService.getLongEmployeeDTOMap();

        return list.stream()
                .collect(Collectors.groupingBy(EvaluationDTO::getTraineeId))
                .entrySet()
                .stream()
                .map(entry -> {

                    Long traineeId = entry.getKey();
                    EmployeeDTO emp = employeeMap.get(traineeId);

                    if (emp == null) return null;

                    return new EvaluationRequestDTO(
                            traineeId,
                            emp.getEmpName(),
                            emp.getEmpDesigName(),
                            emp.getSalutation() != null ? emp.getSalutation() :
                                    (emp.getTitle() != null ? emp.getTitle() : ""),
                            entry.getValue(),
                            null
                    );
                })
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(EvaluationRequestDTO::getEmpName))
                .toList();
    }

    public EvaluationRequestDTO getEvaluationPrint(Long id, String username) {
        log.info("Request to fetch Evaluation print data for id {} by {}", id, username);
        if (id == null) {
            throw new NotFoundException("Employee id cannot be null");
        }

        List<EvaluationDTO> evaluation = evaluationRepository.findByEmployee(id);

        List<EmployeeDTO> employeeDTOList = masterClient.getEmployee(xApiKey, id);
        EmployeeDTO employeeDTO = employeeDTOList.get(0);

        EvaluationRequestDTO requestDTO = new EvaluationRequestDTO();
        requestDTO.setInitiator(id);
        requestDTO.setEmpName(employeeDTO != null ? CommonUtil.buildEmployeeName(employeeDTO, true) : "");
        requestDTO.setEvaluation(evaluation);

        return requestDTO;
    }

    public Optional<OrganizerDTO> getOrganizerById(@NotNull(message = "Organizer is required") Long organizerId) {
        log.info("Request to fetch organizer data for id {}", organizerId);
        return organizerRepository.findById(organizerId).map(organizerMapper::toDto);
    }

    @Transactional(readOnly = true)
    public List<EligibilityDTO> getEligibilityList(String username) {
        log.info("Request to fetch eligibility list by {}", username);
        List<Eligibility> list = eligibilityRepository.findAllByIsActiveOrderByEligibilityIdDesc(1);
        return eligibilityMapper.toDto(list);
    }

    @Transactional
    public EligibilityDTO addEligibleData(@Valid EligibilityDTO dto, String username) {
        log.info("Request to add eligibility name {} by {}", dto.getEligibilityName(), username);
        Eligibility eligibility = eligibilityMapper.toEntity(dto);
        eligibility.setCreatedBy(username);
        eligibility.setCreatedDate(LocalDateTime.now());
        eligibility.setIsActive(1);
        eligibility = eligibilityRepository.save(eligibility);

        return eligibilityMapper.toDto(eligibility);
    }

    @Transactional
    public Optional<EligibilityDTO> updateEligibleData(@Valid EligibilityDTO dto, String username) {
        log.info("Request to update eligibility for id {} by {}", dto.getEligibilityId(), username);

        if (dto.getEligibilityId() == null) {
            throw new NotFoundException("EligibilityId Id can not be null");
        }
        return eligibilityRepository
                .findById(dto.getEligibilityId())
                .map(existingData -> {
                    existingData.setModifiedBy(username);
                    existingData.setModifiedDate(LocalDateTime.now());
                    eligibilityMapper.partialUpdate(existingData, dto);
                    return existingData;
                })
                .map(eligibilityRepository::save)
                .map(eligibilityMapper::toDto);
    }

    @Transactional
    public RequisitionDTO revokeRequisition(@Valid RequisitionDTO dto, String username) {
        log.info("Request to revoke requisition for id {} ", dto.getRequisitionId());

        if (dto.getRequisitionId() == null) {
            throw new NotFoundException("Requisition Id can not be null");
        }

        Requisition requisition = requisitionRepository.findById(dto.getRequisitionId())
                .orElseThrow(() -> new NotFoundException("Requisition not found"));

        requisition.setStatus("REV");
        requisition.setModifiedBy(username);
        requisition.setModifiedDate(LocalDateTime.now());

        insertTransaction(dto.getRequisitionId(), dto.getActionBy(), dto.getActionBy(), username, "REV", null);
        return requisitionMapper.toDto(requisition);
    }

    @CacheEvict(value = "notificationList", allEntries = true)
    @Transactional
    public RequisitionDTO returnRequisition(@Valid RequisitionDTO dto, String username) {
        log.info("Request to return requisition for id {} ", dto.getRequisitionId());

        if (dto.getRequisitionId() == null) {
            throw new NotFoundException("Requisition Id can not be null");
        }

        Requisition requisition = requisitionRepository.findById(dto.getRequisitionId())
                .orElseThrow(() -> new NotFoundException("Requisition not found"));

        CashLimit cashLimit = cashLimitRepository
                .findTopByIsActiveOrderByCashLimitIdDesc(1)
                .orElseThrow(() -> new NotFoundException("Cash limit is not configured."));

        BigDecimal registrationFee = Optional.ofNullable(requisition.getRegistrationFee())
                .orElse(BigDecimal.ZERO);

        BigDecimal limit = Optional.ofNullable(cashLimit)
                .map(CashLimit::getCashLimit)
                .orElse(BigDecimal.ZERO);

        Map<Long, EmployeeDTO> employeeMap = masterCacheService.getLongEmployeeDTOMap();
        EmployeeDTO employeeDTO = employeeMap.get(dto.getActionBy());

        if ("AR".equalsIgnoreCase(requisition.getStatus()) || "AG".equalsIgnoreCase(requisition.getStatus()) || "SF".equalsIgnoreCase(requisition.getStatus())) {
            requisition.setStatus("RS");
            insertTransaction(requisition.getRequisitionId(), dto.getActionBy(), requisition.getInitiatingOfficer(), username, "RS", dto.getRemarks());
        } else if ("AS".equalsIgnoreCase(requisition.getStatus())) {
            if (registrationFee.compareTo(limit) >= 0) {
                requisition.setStatus("CR");
                insertTransaction(requisition.getRequisitionId(), dto.getActionBy(), requisition.getInitiatingOfficer(), username, "CR", dto.getRemarks());
            } else {
                requisition.setStatus("RV");
                insertTransaction(requisition.getRequisitionId(), dto.getActionBy(), requisition.getInitiatingOfficer(), username, "RV", dto.getRemarks());
            }
        } else if ("CA".equalsIgnoreCase(requisition.getStatus())) {
            requisition.setStatus("RV");
            insertTransaction(requisition.getRequisitionId(), dto.getActionBy(), requisition.getInitiatingOfficer(), username, "RV", dto.getRemarks());
        } else {
            requisition.setStatus("RR");
            insertTransaction(requisition.getRequisitionId(), dto.getActionBy(), requisition.getInitiatingOfficer(), username, "RR", dto.getRemarks());
        }
        requisition.setModifiedBy(username);
        requisition.setModifiedDate(LocalDateTime.now());

        String message = getNotificationMsg(requisition.getRequisitionNumber(), employeeDTO, "Returned by");
        insertNotification(dto.getActionBy(), requisition.getInitiatingOfficer(), "requisition", message, username);

        return requisitionMapper.toDto(requisition);
    }

    @Transactional(readOnly = true)
    public FeedbackDTO getFeedbackById(Long id, String username) {
        log.info("Request to fetch feedback data for id {} ", id);

        Feedback feedback = feedbackRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Feedback data not found"));

        FeedbackDTO dto = feedbackMapper.toDto(feedback);
        RequisitionDTO requisitionDTO = getRequisitionById(feedback.getRequisitionId(), username);

        Map<Long, EmployeeDTO> employeeMap = masterCacheService.getLongEmployeeDTOMap();
        EmployeeDTO employeeDTO = employeeMap.get(feedback.getParticipantId());

        if (employeeDTO != null) {
            dto.setParticipantName(CommonUtil.buildEmployeeName(employeeDTO, true));
            dto.setDivisionName(employeeDTO.getEmpDivCode());
        }

        dto.setRequisitionId(requisitionDTO.getRequisitionId());
        dto.setRequisitionNumber(requisitionDTO.getRequisitionNumber());
        dto.setCourseName(requisitionDTO.getCourseName());
        dto.setOrganizer(requisitionDTO.getOrganizer());
        dto.setFromDate(requisitionDTO.getFromDate());
        dto.setToDate(requisitionDTO.getToDate());
        dto.setProgramDuration(requisitionDTO.getDuration());

        return dto;
    }

    @Transactional
    public Optional<FeedbackDTO> updateFeedback(@Valid FeedbackDTO dto, String username) {

        log.info("Request to update feedback for id {} by {}", dto.getFeedbackId(), username);

        if (dto.getFeedbackId() == null) {
            throw new NotFoundException("Feedback Id cannot be null");
        }

        return feedbackRepository
                .findById(dto.getFeedbackId())
                .map(existingData -> {
                    existingData.setModifiedBy(username);
                    existingData.setModifiedDate(LocalDateTime.now());

                    Requisition requisition = requisitionRepository
                            .findById(existingData.getRequisitionId())
                            .orElseThrow(() -> new NotFoundException("Requisition data not found"));

                    Path fullPath = Paths.get(appStorage, "Requisition",
                            requisition.getRequisitionNumber().replace("/", "_"), "Feedback");

                    try {
                        if (dto.getCertificateFile() != null && !dto.getCertificateFile().isEmpty()) {
                            // Delete old certificate if exists
                            if (existingData.getCertificate() != null) {
                                Path oldCertPath = fullPath.resolve(existingData.getCertificate());
                                Files.deleteIfExists(oldCertPath);
                                log.info("Old certificate deleted: {}", oldCertPath);
                            }

                            // Save new file
                            String newFileName = dto.getCertificateFile().getOriginalFilename();
                            FileStorageUtil.saveFile(fullPath, newFileName, dto.getCertificateFile());
                            existingData.setCertificate(newFileName);
                        }


                        if (dto.getInvoiceFile() != null && !dto.getInvoiceFile().isEmpty()) {
                            // Delete old invoice if exists
                            if (existingData.getInvoice() != null) {
                                Path oldInvoicePath = fullPath.resolve(existingData.getInvoice());
                                Files.deleteIfExists(oldInvoicePath);
                                log.info("Old invoice deleted: {}", oldInvoicePath);
                            }

                            // Save new file
                            String newFileName = dto.getInvoiceFile().getOriginalFilename();
                            FileStorageUtil.saveFile(fullPath, newFileName, dto.getInvoiceFile());
                            existingData.setInvoice(newFileName);
                        }

                    } catch (IOException e) {
                        log.error("Error while updating files for feedback {}", dto.getFeedbackId(), e);
                        throw new RuntimeException("File update failed");
                    }

                    feedbackMapper.partialUpdate(existingData, dto);
                    return existingData;
                })
                .map(feedbackRepository::save)
                .map(feedbackMapper::toDto);
    }

    @Transactional
    public FeedbackDTO acceptFeedback(FeedbackDTO dto, String username) {
        log.info("Request to accept feedback for id {} by {}", dto.getFeedbackId(), username);

        if (dto.getFeedbackId() == null) {
            throw new NotFoundException("Feedback Id can not be null");
        }

        Feedback feedback = feedbackRepository.findById(dto.getFeedbackId())
                .orElseThrow(() -> new NotFoundException("Feedback not found"));

        feedback.setIsAccepted("Y");
        feedback.setAcceptedBy(dto.getAcceptedBy());
        feedback.setAcceptedDate(LocalDateTime.now());
        feedback.setModifiedBy(username);
        feedback.setModifiedDate(LocalDateTime.now());

        return feedbackMapper.toDto(feedback);
    }

    public FeedbackDTO getFeedbackPrint(Long id, String username) {
        log.info("Request to print feedback for id {} by {}", id, username);

        if (id == null) {
            throw new NotFoundException("Feedback Id can not be null");
        }

        Feedback feedback = feedbackRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Feedback not found"));

        RequisitionDTO reqDTO = getRequisitionById(feedback.getRequisitionId(), username);

        EmployeeDTO employee = masterClient.getEmployee(xApiKey, feedback.getParticipantId()).get(0);

        FeedbackDTO dto = feedbackMapper.toDto(feedback);

        dto.setRequisitionNumber(reqDTO.getRequisitionNumber());
        dto.setCourseName(reqDTO.getCourseName());
        dto.setFromDate(reqDTO.getFromDate());
        dto.setToDate(reqDTO.getToDate());
        dto.setProgramDuration(reqDTO.getDuration());
        dto.setOrganizer(reqDTO.getOrganizer());
        dto.setParticipantName(CommonUtil.buildEmployeeName(employee, true));
        dto.setDivisionName(employee.getEmpDivCode());

        if ("Y".equalsIgnoreCase(dto.getIsAccepted())) {
            EmployeeDTO acceptEmp = masterClient.getEmployee(xApiKey, feedback.getAcceptedBy()).get(0);
            dto.setAcceptedByName(CommonUtil.buildEmployeeName(acceptEmp, true));
        }

        return dto;
    }

    @Cacheable(value = "getCourseTypeList")
    public List<CourseTypeDTO> getCourseTypeList(String username) {
        log.info("Course type list fetched by {}", username);
        List<CourseType> typeList = courseTypeRepository.findAllByIsActive(1);

        return typeList.stream()
                .map(data -> {
                    CourseTypeDTO typeDTO = new CourseTypeDTO();
                    typeDTO.setCourseTypeId(data.getCourseTypeId());
                    typeDTO.setCourseType(data.getCourseType());
                    return typeDTO;
                }).toList();
    }

    public List<RequisitionDTO> getRequisitionApprovedList(Long empId, String username) {
        log.info("Requisition approved list fetched by {}", username);

        LocalDate today = LocalDate.now();

        // SA_HRT Authorities
        Set<Long> saHrtEmpIds = signRoleAuthorityRepository
                .findBySignAuthRole("SA-HRT")
                .stream()
                .map(SignRoleAuthorityDTO::getEmpId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // Active handovers received by current employee
        Set<Long> handedOverFromEmpIds = handingOverRepository
                .findByToEmpIdAndToDateGreaterThanEqualAndIsActive(
                        empId,
                        today,
                        1)
                .stream()
                .map(HandingOver::getFromEmpId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        boolean isSAHRT =
                saHrtEmpIds.contains(empId)
                        || handedOverFromEmpIds.stream()
                        .anyMatch(saHrtEmpIds::contains);


        List<String> statusCodes;

        if (isSAHRT) {
            statusCodes = Arrays.asList("AV", "DA");
        } else {
            throw new RuntimeException("Access Denied");
        }

        List<Requisition> list = requisitionRepository.findAllByStatusInAndIsActive(statusCodes, 1);

        List<RequisitionDTO> dtoList = requisitionMapper.toDto(list);

        Map<String, Status> statusMap = masterCacheService.getStatusMap();
        Map<Long, Organizer> organizerMap = masterCacheService.getOrganizerMap();
        Map<Long, Course> courseMap = masterCacheService.getCourseMap();
        Map<Long, EmployeeDTO> employeeMap = masterCacheService.getLongEmployeeDTOMap();

        for (RequisitionDTO dto : dtoList) {

            // Course + Organizer
            Course course = courseMap.get(dto.getCourseId());
            if (course != null) {

                dto.setCourseName(course.getCourseName());
                dto.setVenue(course.getVenue());
                dto.setOfflineRegistrationFee(course.getOfflineRegistrationFee());
                dto.setOnlineRegistrationFee(course.getOnlineRegistrationFee());

                Organizer organizer = organizerMap.get(course.getOrganizerId());
                if (organizer != null) {
                    dto.setOrganizerId(organizer.getOrganizerId());
                    dto.setOrganizer(organizer.getOrganizer());
                    dto.setOrganizerContactName(organizer.getContactName());
                    dto.setOrganizerPhoneNo(organizer.getPhoneNo());
                    dto.setOrganizerFaxNo(organizer.getFaxNo());
                    dto.setOrganizerEmail(organizer.getEmail());
                }
            }

            Status status = statusMap.get(dto.getStatus());
            if (status != null) {
                dto.setStatusName(status.getStatusName());
                dto.setStatusColor(status.getColorCode());
            }

            if (dto.getInitiatingOfficer() != null) {
                EmployeeDTO employeeDTO = employeeMap.get(dto.getInitiatingOfficer());
                if (employeeDTO != null) {
                    dto.setInitiatingOfficerName(CommonUtil.buildEmployeeName(employeeDTO, false));
                    dto.setEmpDesigName(employeeDTO.getEmpDesigName());
                    dto.setIsGroup(employeeDTO.getIsGroup());
                }
            }
        }

        return dtoList;
    }

    @Transactional
    public DirectorApproveDTO reqForwardToDirector(DirectorApproveDTO dto, String username) {
        log.info("Requisitions forward to director for ids {} by {} ", dto, username);

        if (dto == null || dto.getRequisitionIds() == null || dto.getRequisitionIds().isEmpty()) {
            throw new NotFoundException("Requisition Id list cannot be empty");
        }

        List<Requisition> requisitions = requisitionRepository.findAllById(dto.getRequisitionIds());

        if (requisitions.isEmpty()) {
            throw new NotFoundException("No requisitions found for given ids");
        }

        LoginEmployeeDto login = loginRepository.findEmployeeByRoleName("ROLE_DIRECTOR");

        if (login == null || login.getLoginId() == null) {
            throw new NotFoundException("ROLE_DIRECTOR not found");
        }

        Map<Long, EmployeeDTO> employeeMap = masterCacheService.getLongEmployeeDTOMap();

        for (Requisition req : requisitions) {

            EmployeeDTO employeeDTO = employeeMap.get(dto.getActionBy());
            String message = getNotificationMsg(req.getRequisitionNumber(), employeeDTO, "Forward by");

            if (req.getStatus().equalsIgnoreCase("DA")) {
                req.setStatus("FC");
                insertTransaction(req.getRequisitionId(), dto.getActionBy(), login.getEmpId(), username, "FC", null);
                insertNotification(dto.getActionBy(), login.getEmpId(), "req-approved-list", message, username);
            } else {
                req.setStatus("AD");
                insertTransaction(req.getRequisitionId(), dto.getActionBy(), login.getEmpId(), username, "AD", null);
                insertNotification(dto.getActionBy(), login.getEmpId(), "req-approved-list", message, username);
            }
            req.setModifiedBy(username);
            req.setModifiedDate(LocalDateTime.now());
        }

        requisitionRepository.saveAll(requisitions);
        return dto;
    }

    @Transactional
    public DirectorApproveDTO approveRequisition(DirectorApproveDTO dto, String username) {
        log.info("Requisitions approved by director for ids {} by {} ", dto, username);

        if (dto == null || dto.getRequisitionIds() == null || dto.getRequisitionIds().isEmpty()) {
            throw new NotFoundException("Requisition Id list cannot be empty");
        }

        List<Requisition> requisitions = requisitionRepository.findAllById(dto.getRequisitionIds());

        if (requisitions.isEmpty()) {
            throw new NotFoundException("No requisitions found for given ids");
        }

        for (Requisition req : requisitions) {
            if ("FC".equalsIgnoreCase(req.getStatus())) {
                req.setStatus("FA");
                insertTransaction(req.getRequisitionId(), dto.getActionBy(), dto.getActionBy(), username, "FA", null);
            } else {
                req.setStatus("CO");
                insertTransaction(req.getRequisitionId(), dto.getActionBy(), dto.getActionBy(), username, "CO", null);
            }
            req.setModifiedBy(username);
            req.setModifiedDate(LocalDateTime.now());
        }

        requisitionRepository.saveAll(requisitions);
        return dto;
    }

    public DirectorApproveDTO recommendReqToDFA(DirectorApproveDTO dto, String username) {
        log.info("Requisitions recommend to DFA by director for ids {} by {} ", dto, username);

        if (dto == null || dto.getRequisitionIds() == null || dto.getRequisitionIds().isEmpty()) {
            throw new NotFoundException("Requisition Id list cannot be empty");
        }

        List<Requisition> requisitions = requisitionRepository.findAllById(dto.getRequisitionIds());

        if (requisitions.isEmpty()) {
            throw new NotFoundException("No requisitions found for given ids");
        }

        for (Requisition req : requisitions) {
            req.setStatus("DA");
            req.setModifiedBy(username);
            req.setModifiedDate(LocalDateTime.now());
            insertTransaction(req.getRequisitionId(), dto.getActionBy(), dto.getActionBy(), username, "DA", null);
        }

        requisitionRepository.saveAll(requisitions);
        return dto;
    }

    private void insertTransaction(Long id, Long actionBy, Long actionTo, String username, String status, String remarks) {
        RequisitionTransaction transaction = new RequisitionTransaction();
        transaction.setRequisitionId(id);
        transaction.setStatusCode(status);
        transaction.setActionBy(actionBy);
        transaction.setActionTo(actionTo);
        transaction.setActionDate(LocalDateTime.now());
        transaction.setRemarks(remarks);
        transaction.setCreatedBy(username);
        transaction.setCreatedDate(LocalDateTime.now());
        transaction.setIsActive(1);
        transactionRepository.save(transaction);
    }


    private void insertNotification(Long actionBy, Long actionTo, String url, String message, String username) {
        Notification notification = new Notification();
        notification.setNotificationBy(actionBy);
        notification.setEmpId(actionTo);
        notification.setNotificationDate(LocalDate.now());
        notification.setNotificationUrl(url);
        notification.setNotificationMessage(message);
        notification.setCreatedBy(username);
        notification.setCreatedDate(LocalDateTime.now());
        notification.setIsActive(1);
        notificationRepository.save(notification);
    }

    @Cacheable(value = "cepListCache", key = "#username")
    public List<CepDTO> getAllCepData(String username) {
        log.info("Request to fetch CEP list by {}", username);

        List<Cep> cepList = cepRepository.findAllByIsActiveOrderByCepIdDesc(1);
        List<CepDTO> dtoList = cepMapper.toDto(cepList);

        Map<Long, DivisionDTO> divisionDTOMap = masterCacheService.getDivisionDTOMap();
        Map<Long, EmployeeDTO> employeeDTOMap = masterCacheService.getLongEmployeeDTOMap();

        dtoList.forEach(data -> {
            DivisionDTO divisionDTO = divisionDTOMap.get(data.getDivisionId());

            EmployeeDTO coordinator = employeeDTOMap.get(data.getCourseCoordinatorId());
            EmployeeDTO deputyCoordinator = employeeDTOMap.get(data.getDeputyCourseCoordinatorId());

            data.setDivisionCode(divisionDTO.getDivisionShortName());
            data.setCourseCoordinatorName(CommonUtil.buildEmployeeName(coordinator, true));
            data.setDeputyCourseCoordinatorName(CommonUtil.buildEmployeeName(deputyCoordinator, true));
        });
        return dtoList;
    }

    public CepDTO getCepID(Long cepId, String username) {
        log.info("Request to fetch CEP by Id {} by {}", cepId, username);

        if (cepId == null) {
            throw new NotFoundException("CEP id cannot be null");
        }

        Cep cep = cepRepository.findById(cepId)
                .orElseThrow(() -> new NotFoundException("CEP data not found"));

        CepDTO dto = cepMapper.toDto(cep);

        List<CepAttachments> attachments = cepAttachmentsRepository.findByCepId(cepId);
        List<CepAttachmentsDTO> dtoList = new ArrayList<>();
        for (CepAttachments cepData : attachments) {
            CepAttachmentsDTO attachDto = new CepAttachmentsDTO();
            attachDto.setAttachmentId(cepData.getAttachmentId());
            attachDto.setAttachmentName(cepData.getAttachmentName());
            attachDto.setExistingFileName(cepData.getAttachFile());
            dtoList.add(attachDto);
        }

        dto.setCepAttachments(dtoList);
        return dto;
    }


    @CacheEvict(value = {"cepListCache", "cepReportCache"}, allEntries = true)
    @Transactional
    public CepDTO addCepData(@Valid CepDTO dto, String username) throws IOException {
        log.info("Request to add CEP by {}", username);

        if (dto.getFromDate() != null && dto.getToDate() != null && dto.getToDate().isBefore(dto.getFromDate())) {
            throw new IllegalArgumentException("To date cannot be before From date");
        }

        if (dto.getAmountSpent() != null && dto.getTotalAmount() != null &&
                dto.getAmountSpent().compareTo(dto.getTotalAmount()) > 0) {
            throw new IllegalArgumentException("Amount spent cannot exceed total amount");
        }

        Cep cep = cepMapper.toEntity(dto);
        cep.setCreatedBy(username);
        cep.setCreatedDate(LocalDateTime.now());
        cep.setIsActive(1);

        cep = cepRepository.save(cep);

        List<CepAttachments> attachments = new ArrayList<>();

        for (CepAttachmentsDTO attachmentsDTO : dto.getCepAttachments()) {
            CepAttachments attach = new CepAttachments();
            attach.setCepId(cep.getCepId());
            attach.setAttachmentName(attachmentsDTO.getAttachmentName());

            if (attachmentsDTO.getAttachFile() != null && !attachmentsDTO.getAttachFile().isEmpty()) {
                String folderName = "CEP_" + cep.getCepId();
                Path filepath = Paths.get(appStorage, "CEP Attachments", folderName);
                FileStorageUtil.saveFile(filepath, attachmentsDTO.getAttachFile().getOriginalFilename(), attachmentsDTO.getAttachFile());
                attach.setAttachFile(attachmentsDTO.getAttachFile().getOriginalFilename());
            }

            attach.setCreatedBy(username);
            attach.setCreatedDate(LocalDateTime.now());
            attach.setIsActive(1);
            attachments.add(attach);
        }
        cepAttachmentsRepository.saveAll(attachments);

        return cepMapper.toDto(cep);
    }

    @CacheEvict(value = {"cepListCache", "cepReportCache"}, allEntries = true)
    @Transactional
    public Optional<CepDTO> editCEPData(@Valid CepDTO dto, String username) throws IOException {

        log.info("Request to edit CEP for id {} by {}", dto.getCepId(), username);

        // 👉 Validations
        if (dto.getFromDate() != null && dto.getToDate() != null &&
                dto.getToDate().isBefore(dto.getFromDate())) {
            throw new BadRequestException("To date cannot be before From date");
        }

        if (dto.getAmountSpent() != null && dto.getTotalAmount() != null &&
                dto.getAmountSpent().compareTo(dto.getTotalAmount()) > 0) {
            throw new BadRequestException("Amount spent cannot exceed total amount");
        }

        // 👉 Fetch existing attachments
        List<CepAttachments> existingAttachments =
                cepAttachmentsRepository.findByCepId(dto.getCepId());

        List<CepAttachments> updatedAttachments = new ArrayList<>();

        // 👉 Folder path
        String folderName = "CEP_" + dto.getCepId();
        Path filepath = Paths.get(appStorage, "CEP Attachments", folderName);

        // =========================================================
        // 👉 1. HANDLE ADD / UPDATE
        // =========================================================
        for (CepAttachmentsDTO attachmentsDTO : dto.getCepAttachments()) {

            // Skip empty rows (important)
            if ((attachmentsDTO.getAttachmentName() == null || attachmentsDTO.getAttachmentName().isBlank())
                    && (attachmentsDTO.getAttachFile() == null || attachmentsDTO.getAttachFile().isEmpty())
                    && attachmentsDTO.getAttachmentId() == null) {
                continue;
            }

            CepAttachments attach;

            // 👉 EXISTING
            if (attachmentsDTO.getAttachmentId() != null) {

                attach = existingAttachments.stream()
                        .filter(a -> a.getAttachmentId().equals(attachmentsDTO.getAttachmentId()))
                        .findFirst()
                        .orElseThrow(() -> new BadRequestException("Attachment not found"));

                attach.setModifiedBy(username);
                attach.setModifiedDate(LocalDateTime.now());

            } else {
                // 👉 NEW
                attach = new CepAttachments();
                attach.setCepId(dto.getCepId());
                attach.setCreatedBy(username);
                attach.setCreatedDate(LocalDateTime.now());
                attach.setIsActive(1);
            }

            // 👉 Update name
            attach.setAttachmentName(attachmentsDTO.getAttachmentName());

            // 👉 FILE REPLACEMENT
            if (attachmentsDTO.getAttachFile() != null &&
                    !attachmentsDTO.getAttachFile().isEmpty()) {

                // Delete old file if exists
                if (attach.getAttachFile() != null) {
                    FileStorageUtil.deleteFileIfExists(filepath, attach.getAttachFile());
                }

                // Save new file
                String fileName = attachmentsDTO.getAttachFile().getOriginalFilename();

                FileStorageUtil.saveFile(
                        filepath,
                        fileName,
                        attachmentsDTO.getAttachFile()
                );

                attach.setAttachFile(fileName);
            }

            updatedAttachments.add(attach);
        }

        // =========================================================
        // 👉 2. HANDLE DELETE (REMOVED FROM UI)
        // =========================================================
        List<Long> incomingIds = dto.getCepAttachments().stream()
                .filter(a -> a.getAttachmentId() != null)
                .map(CepAttachmentsDTO::getAttachmentId)
                .toList();

        for (CepAttachments existing : existingAttachments) {

            if (existing.getAttachmentId() != null &&
                    !incomingIds.contains(existing.getAttachmentId())) {

                // Delete file
                if (existing.getAttachFile() != null) {
                    FileStorageUtil.deleteFileIfExists(filepath, existing.getAttachFile());
                }

                // Delete DB record
                cepAttachmentsRepository.delete(existing);
            }
        }

        // 👉 Save updated & new attachments
        cepAttachmentsRepository.saveAll(updatedAttachments);

        // =========================================================
        // 👉 3. UPDATE MAIN CEP
        // =========================================================
        return cepRepository.findById(dto.getCepId())
                .map(existing -> {
                    existing.setModifiedBy(username);
                    existing.setModifiedDate(LocalDateTime.now());
                    cepMapper.partialUpdate(existing, dto);
                    return existing;
                })
                .map(cepRepository::save)
                .map(cepMapper::toDto);
    }

    @Cacheable(value = "distributionCache", key = "#username")
    public List<DistributionDTO> getAllDistributionsData(String username) {
        log.info("Request to fetch Distribution list by {}", username);

        List<Distribution> list = distributionRepository.findAllByIsActive(1);
        List<DistributionDTO> dtoList = distributionMapper.toDto(list);

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

        });

        return dtoList;
    }

    public DistributionDTO getDistributionByID(Long distributionId, String username) {
        log.info("Request to fetch Distribution by id {} by {}", distributionId, username);

        if (distributionId == null) {
            throw new NotFoundException("Distribution id cannot be null");
        }
        Distribution distribution = distributionRepository.findById(distributionId)
                .orElseThrow(() -> new NotFoundException("Distribution data not found"));

        return distributionMapper.toDto(distribution);
    }

    @CacheEvict(value = {"distributionCache", "hrDistributionReportCache"}, allEntries = true)
    @Transactional
    public DistributionDTO addDistributionData(@Valid DistributionDTO dto, String username) {
        log.info("Request to add Distribution by {}", username);

        Distribution distribution = distributionMapper.toEntity(dto);

        distribution.setDistributionDate(LocalDate.now());
        distribution.setCreatedBy(username);
        distribution.setCreatedDate(LocalDateTime.now());
        distribution.setIsActive(1);

        distribution = distributionRepository.save(distribution);
        return distributionMapper.toDto(distribution);
    }

    @CacheEvict(value = {"distributionCache", "hrDistributionReportCache"}, allEntries = true)
    public Optional<DistributionDTO> editDistributionData(@Valid DistributionDTO dto, String username) {
        log.info("Request to edit distribution id {} by {}", dto.getDistributionId(), username);

        return distributionRepository.findById(dto.getDistributionId())
                .map(ex -> {
                    ex.setModifiedBy(username);
                    ex.setModifiedDate(LocalDateTime.now());
                    distributionMapper.partialUpdate(ex, dto);
                    return ex;
                })
                .map(distributionRepository::save)
                .map(distributionMapper::toDto);
    }


    public Optional<CalendarDTO> updateCalendarData(@Valid CalendarDTO dto, String username) {
        log.info("Request to update calendar for id {} by {}", dto.getCalendarId(), username);

        OrganizerDTO organizerDTO = getOrganizerById(dto.getOrganizerId())
                .orElseThrow(() -> new NotFoundException("Organizer data not found"));

        Path fullpath = Paths.get(appStorage, "Calendar", dto.getYear(), organizerDTO.getOrganizer().trim());

        return calenderRepository
                .findById(dto.getCalendarId())
                .map(existingReq -> {
                    existingReq.setModifiedBy(username);
                    existingReq.setModifiedDate(LocalDateTime.now());

                    if (dto.getFile() != null && !dto.getFile().isEmpty()) {
                        updateFile(dto.getFile(), existingReq.getCalendarFileName(), fullpath, existingReq::setCalendarFileName);
                    }
                    if (dto.getCoverFile() != null && !dto.getCoverFile().isEmpty()) {
                        updateFile(dto.getCoverFile(), existingReq.getCoveringLetter(), fullpath, existingReq::setCoveringLetter);
                    }

                    calenderMapper.partialUpdate(existingReq, dto);
                    return existingReq;
                })
                .map(calenderRepository::save)
                .map(calenderMapper::toDto);

    }

    public CepAttachmentsDTO getCepAttachmentById(Long attachmentId, String username) {
        log.info("Request to get CEp attachment data for id {} by {}", attachmentId, username);

        CepAttachments entity = cepAttachmentsRepository.findById(attachmentId)
                .orElseThrow(() -> new NotFoundException("CEP Attachment data not found"));

        CepAttachmentsDTO dto = new CepAttachmentsDTO();

        dto.setCepId(entity.getCepId());
        dto.setAttachmentId(entity.getAttachmentId());
        dto.setAttachmentName(entity.getAttachmentName());
        dto.setExistingFileName(entity.getAttachFile());

        return dto;
    }

    @Cacheable(value = "journalCache", key = "#username")
    public List<JournalDTO> getJournalList(Long empId, String roleName, String username) {
        log.info("Request to fetch journal list by {}", username);

        List<Journal> journals;

        if ("ROLE_USER".equalsIgnoreCase(roleName)) {
            journals = journalRepository.findAllByEmpIdAndIsActiveOrderByJournalIdDesc(empId, 1);
        } else {
            journals = journalRepository.findAllByIsActiveOrderByJournalIdDesc(1);
        }

        List<JournalDTO> dtoList = journalMapper.toDto(journals);

        Map<Long, EmployeeDTO> employeeDTOMap = masterCacheService.getLongEmployeeDTOMap();

        dtoList.forEach(data -> {

            EmployeeDTO empDto = employeeDTOMap.get(data.getEmpId());
            if (empDto != null) {
                data.setEmployeeName(CommonUtil.buildEmployeeName(empDto, true));
                data.setEmpNo(empDto.getEmpNo());
                data.setDesigCadre(empDto.getDesigCadre());
            }
        });
        return dtoList;
    }

    @CacheEvict(value = "journalCache", allEntries = true)
    @Transactional
    public JournalDTO addJournalData(@Valid JournalDTO dto, String username) {
        log.info("Request to add journal by {}", username);

        Journal journal = journalMapper.toEntity(dto);

        journal.setCreatedBy(username);
        journal.setCreatedDate(LocalDateTime.now());
        journal.setIsActive(1);

        journal = journalRepository.save(journal);
        return journalMapper.toDto(journal);
    }

    @CacheEvict(value = "journalCache", allEntries = true)
    @Transactional
    public Optional<JournalDTO> editJournalData(@Valid JournalDTO dto, String username) {
        log.info("Request to update journal for id {} by {}", dto.getJournalId(), username);

        return journalRepository.findById(dto.getJournalId())
                .map(ex -> {
                    ex.setModifiedBy(username);
                    ex.setModifiedDate(LocalDateTime.now());
                    journalMapper.partialUpdate(ex, dto);
                    return ex;
                })
                .map(journalRepository::save)
                .map(journalMapper::toDto);
    }

    @Transactional
    public RequisitionDTO acceptMandatoryTraining(RequisitionDTO dto, String username) {
        log.info("Request to accept mandatory training for id {} by {}", dto.getRequisitionId(), username);

        if (dto.getRequisitionId() == null) {
            throw new NotFoundException("Requisition id cannot be null");
        }

        Requisition requisition = requisitionRepository.findById(dto.getRequisitionId())
                .orElseThrow(() -> new NotFoundException("Requisition data not found"));

        requisition.setStatus("PA");
        requisition.setModifiedBy(username);
        requisition.setModifiedDate(LocalDateTime.now());

        insertTransaction(requisition.getRequisitionId(), dto.getActionBy(), dto.getActionBy(), username, "PA", null);

        return requisitionMapper.toDto(requisition);
    }

    public List<RequisitionDTO> getMandatoryTrainingByParticipantId(Long id, String username) {
        log.info("Request to fetch mandatory training by participant id {} by {}", id, username);
        if (id == null) {
            throw new NotFoundException("Participant id cannot be null");
        }

        Map<Long, Course> courseMap = masterCacheService.getCourseMap();
        List<Requisition> list = requisitionRepository.findAllByInitiatingOfficerAndIsActiveOrderByRequisitionIdDesc(id, 1);

        List<RequisitionDTO> dtoList = requisitionMapper.toDto(list);
        dtoList.forEach(data -> {
            Course course = courseMap.get(data.getCourseId());
            if (course != null) data.setCourseName(course.getCourseName());
        });
        return dtoList;
    }

    public List<RequisitionDTO> getApprovedListByEmpId(Long empId, LocalDate fromDate, LocalDate toDate) {

        log.info("Request to fetch getRoleWiseApprovedList empId: {}, fromDate: {}, toDate: {}", empId, fromDate, toDate);

        List<RequisitionTransaction> transListByEmpId =
                transactionRepository.findTransactionsByEmployeeAndDateRange(empId, fromDate.atStartOfDay(), toDate.atTime(LocalTime.MAX), List.of("AR", "AG", "AV", "AS", "CA"));

        Map<Long, RequisitionTransaction> transactionMap = transListByEmpId.stream()
                .collect(Collectors.toMap(
                        RequisitionTransaction::getRequisitionId,
                        Function.identity(),
                        BinaryOperator.maxBy(
                                Comparator.comparing(RequisitionTransaction::getTransactionId)
                        )
                ));

        Set<Long> requisitionIds = transListByEmpId
                .stream()
                .map(RequisitionTransaction::getRequisitionId)
                .collect(Collectors.toSet());

        if (requisitionIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<Requisition> requisitionList = requisitionRepository.findAllByRequisitionIdIn(requisitionIds);

        if (requisitionList.isEmpty()) {
            return Collections.emptyList();
        }

        List<RequisitionDTO> dtoList = requisitionMapper.toDto(requisitionList);
        // Fetch master data

        Map<String, Status> statusMap = masterCacheService.getStatusMap();
        Map<Long, Organizer> organizerMap = masterCacheService.getOrganizerMap();
        Map<Long, Course> courseMap = masterCacheService.getCourseMap();
        Map<Long, EmployeeDTO> employeeMap = masterCacheService.getLongEmployeeDTOMap();


        for (RequisitionDTO dto : dtoList) {

            if (dto.getInitiatingOfficer() != null) {
                EmployeeDTO employeeDTO = employeeMap.get(dto.getInitiatingOfficer());
                if (employeeDTO != null) {
                    dto.setInitiatingOfficerName(CommonUtil.buildEmployeeName(employeeDTO, false));
                    dto.setEmpDesigName(employeeDTO.getEmpDesigName());
                    dto.setIsGroup(employeeDTO.getIsGroup());
                }
            }

            // Course + Organizer
            Course course = courseMap.get(dto.getCourseId());
            if (course != null) {
                dto.setCourseName(course.getCourseName());
                Organizer organizer = organizerMap.get(course.getOrganizerId());
                if (organizer != null) {
                    dto.setOrganizerId(organizer.getOrganizerId());
                    dto.setOrganizer(organizer.getOrganizer());
                }
            }

            Status status = statusMap.get(dto.getStatus());
            if (status != null) {
                dto.setStatusName(status.getStatusName());
                dto.setStatusColor(status.getColorCode());
            }

            RequisitionTransaction requisitionTransaction = transactionMap.get(dto.getRequisitionId());
            if (requisitionTransaction != null) {
                dto.setApprovedDate(requisitionTransaction.getActionDate());
            }

        }

        return dtoList;

    }

    public String getApprovalType(Long empId) {

        LocalDate today = LocalDate.now();

        Set<Long> saHrtEmpIds = signRoleAuthorityRepository
                .findBySignAuthRole("SA-HRT")
                .stream()
                .map(SignRoleAuthorityDTO::getEmpId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        LoginEmployeeDto directorEmp =
                loginRepository.findEmployeeByRoleName("ROLE_DIRECTOR");

        Long directorEmpId =
                directorEmp != null ? directorEmp.getEmpId() : null;

        Set<Long> handedOverFromEmpIds = handingOverRepository
                .findByToEmpIdAndToDateGreaterThanEqualAndIsActive(
                        empId,
                        today,
                        1)
                .stream()
                .map(HandingOver::getFromEmpId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // Director first
        if (Objects.equals(empId, directorEmpId)
                || handedOverFromEmpIds.contains(directorEmpId)) {
            return "DIRECTOR";
        }

        // Then SA_HRT
        if (saHrtEmpIds.contains(empId)
                || handedOverFromEmpIds.stream()
                .anyMatch(saHrtEmpIds::contains)) {
            return "SA_HRT";
        }

        return "NO_ACCESS";
    }

    public RequisitionDTO addConfirmation(@Valid RequisitionDTO dto, String username) throws IOException {
        log.info("Request to add confirmation for reqId {} by {} ", dto.getRequisitionId(), username);

        Requisition requisition = requisitionRepository.findById(dto.getRequisitionId())
                .orElseThrow(() -> new NotFoundException("Requisition data not found"));

        requisition.setIsConfirmed(dto.getIsConfirmed());
        requisition.setConfirmRemarks(dto.getConfirmRemarks());
        requisition.setModifiedBy(username);
        requisition.setModifiedDate(LocalDateTime.now());

        Path fullpath = Paths.get(appStorage, "Requisition", requisition.getRequisitionNumber().replace("/", "_"));
        if (dto.getMultipartFileBrochure() != null && !dto.getMultipartFileBrochure().isEmpty()) {
            requisition.setFileConfirm(dto.getMultipartFileBrochure().getOriginalFilename());
            FileStorageUtil.saveFile(fullpath, dto.getMultipartFileBrochure().getOriginalFilename(), dto.getMultipartFileBrochure());
        }
        requisition = requisitionRepository.save(requisition);
        return requisitionMapper.toDto(requisition);
    }

    public RequisitionDTO addReqAttendance(RequisitionDTO dto, String username) {
        log.info("Request to add attendance for reqId {} by {} ", dto.getRequisitionId(), username);

        Requisition requisition = requisitionRepository.findById(dto.getRequisitionId())
                .orElseThrow(() -> new NotFoundException("Requisition data not found"));

        if ("N".equalsIgnoreCase(dto.getIsConfirmed())) {
            requisition.setIsConfirmed("Y");
        }

        requisition.setIsAttend(dto.getIsAttend());
        requisition.setAttendRemarks(dto.getAttendRemarks());
        requisition.setModifiedBy(username);
        requisition.setModifiedDate(LocalDateTime.now());

        requisition = requisitionRepository.save(requisition);
        return requisitionMapper.toDto(requisition);
    }

    public LabMasterDTO getLabMasterData(String username) {
        log.info("Request to fetch lab master data by {} ", username);
        return masterCacheService.getLabMasterData()
                .orElseThrow(() -> new NotFoundException("Lab Master data not found."));
    }

    public List<FeedbackDTO> getFeedbackListByDateRange(Long empId, String roleName, LocalDate fromDate, LocalDate toDate) {
        log.info("Feedback list fetched by empId {} fromDate {} toDate {}", empId, fromDate, toDate);

        List<Feedback> feedbackList;
        List<EmployeeDTO> employeeList = masterClient.getEmployeeMasterList(xApiKey);

        if (Arrays.asList("ROLE_ADMIN", "ROLE_AD_HRT", "ROLE_SA_HRT", "ROLE_DIRECTOR",
                "ROLE_CAG_DIV", "ROLE_TCG_DIV", "ROLE_SM_HRT").contains(roleName) && empId == 0) {

            feedbackList = feedbackRepository.findByDateRange(fromDate, toDate);

        } else if ("ROLE_DH".equalsIgnoreCase(roleName) || "ROLE_GH".equalsIgnoreCase(roleName)) {

            List<DivisionDTO> divisionList = masterClient.getDivisionMaster(xApiKey);

            Optional<DivisionDTO> divisionOpt = divisionList.stream()
                    .filter(d -> Objects.equals(d.getDivisionHeadId(), empId))
                    .findFirst();

            if (divisionOpt.isPresent()) {

                Long divisionId = divisionOpt.get().getDivisionId();
                List<Long> empIds = employeeList.stream()
                        .filter(e -> labCode != null && labCode.equalsIgnoreCase(e.getLabCode()))
                        .filter(emp -> Objects.equals(emp.getDivisionId(), divisionId))
                        .map(EmployeeDTO::getEmpId)
                        .collect(Collectors.toList());

                feedbackList = feedbackRepository.findAllActiveByParticipantsAndDateRange(empIds, fromDate, toDate);

            } else {
                feedbackList = new ArrayList<>();
            }

        } else {
            feedbackList = feedbackRepository.findAllActiveByParticipantAndDateRange(empId, fromDate, toDate);
        }

        if (feedbackList == null || feedbackList.isEmpty()) {
            return Collections.emptyList();
        }

        List<FeedbackDTO> feedbbackdto = feedbackMapper.toDto(feedbackList);

        if (feedbbackdto == null || feedbbackdto.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, EmployeeDTO> employeeMap = employeeList != null
                ? employeeList.stream()
                .filter(e -> labCode != null && labCode.equalsIgnoreCase(e.getLabCode()))
                .collect(Collectors.toMap(EmployeeDTO::getEmpId, emp -> emp))
                : Collections.emptyMap();

        feedbbackdto.forEach(d -> {

            if (d == null) return;

            EmployeeDTO employeeDTO = employeeMap.get(d.getParticipantId());

            if (employeeDTO != null) {
                d.setParticipantName(CommonUtil.buildEmployeeName(employeeDTO, true));
                d.setDivisionName(employeeDTO.getEmpDivCode());
                d.setIsGroup(employeeDTO.getIsGroup());
            }

            RequisitionDTO requisitionDto = null;

            if (d.getRequisitionId() != null) {
                requisitionDto = getRequisitionById(d.getRequisitionId(), null);
            }

            if (requisitionDto != null) {
                d.setRequisitionNumber(requisitionDto.getRequisitionNumber());
                d.setCourseName(requisitionDto.getCourseName());
                d.setOrganizer(requisitionDto.getOrganizer());
                d.setFromDate(requisitionDto.getFromDate());
                d.setToDate(requisitionDto.getToDate());
                d.setProgramDuration(requisitionDto.getDuration());
            }

        });

        return feedbbackdto;
    }

    public List<RequisitionDTO> getRequisitionDirectorApprovedList(Long empId, String username) {
        log.info("Requisition director approved list fetched by {}", username);

        LocalDate today = LocalDate.now();

        // SA_HRT Authorities
        Set<Long> saHrtEmpIds = signRoleAuthorityRepository
                .findBySignAuthRole("SA-HRT")
                .stream()
                .map(SignRoleAuthorityDTO::getEmpId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // Director
        LoginEmployeeDto directorEmp =
                loginRepository.findEmployeeByRoleName("ROLE_DIRECTOR");

        Long directorEmpId =
                directorEmp != null ? directorEmp.getEmpId() : null;

        // Active handovers received by current employee
        Set<Long> handedOverFromEmpIds = handingOverRepository
                .findByToEmpIdAndToDateGreaterThanEqualAndIsActive(
                        empId,
                        today,
                        1)
                .stream()
                .map(HandingOver::getFromEmpId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        boolean isSAHRT =
                saHrtEmpIds.contains(empId)
                        || handedOverFromEmpIds.stream()
                        .anyMatch(saHrtEmpIds::contains);

        boolean isDirector =
                Objects.equals(empId, directorEmpId)
                        || handedOverFromEmpIds.contains(directorEmpId);

        List<String> statusCodes;

        if (isSAHRT || isDirector) {
            statusCodes = Arrays.asList("AD", "FC");
        } else {
            throw new RuntimeException("Access Denied");
        }

        List<Requisition> list = requisitionRepository.findAllByStatusInAndIsActive(statusCodes, 1);

        List<RequisitionDTO> dtoList = requisitionMapper.toDto(list);

        Map<String, Status> statusMap = masterCacheService.getStatusMap();
        Map<Long, Organizer> organizerMap = masterCacheService.getOrganizerMap();
        Map<Long, Course> courseMap = masterCacheService.getCourseMap();
        Map<Long, EmployeeDTO> employeeMap = masterCacheService.getLongEmployeeDTOMap();

        for (RequisitionDTO dto : dtoList) {

            // Course + Organizer
            Course course = courseMap.get(dto.getCourseId());
            if (course != null) {

                dto.setCourseName(course.getCourseName());
                dto.setVenue(course.getVenue());
                dto.setOfflineRegistrationFee(course.getOfflineRegistrationFee());
                dto.setOnlineRegistrationFee(course.getOnlineRegistrationFee());

                Organizer organizer = organizerMap.get(course.getOrganizerId());
                if (organizer != null) {
                    dto.setOrganizerId(organizer.getOrganizerId());
                    dto.setOrganizer(organizer.getOrganizer());
                    dto.setOrganizerContactName(organizer.getContactName());
                    dto.setOrganizerPhoneNo(organizer.getPhoneNo());
                    dto.setOrganizerFaxNo(organizer.getFaxNo());
                    dto.setOrganizerEmail(organizer.getEmail());
                }
            }

            Status status = statusMap.get(dto.getStatus());
            if (status != null) {
                dto.setStatusName(status.getStatusName());
                dto.setStatusColor(status.getColorCode());
            }

            if (dto.getInitiatingOfficer() != null) {
                EmployeeDTO employeeDTO = employeeMap.get(dto.getInitiatingOfficer());
                if (employeeDTO != null) {
                    dto.setInitiatingOfficerName(CommonUtil.buildEmployeeName(employeeDTO, false));
                    dto.setEmpDesigName(employeeDTO.getEmpDesigName());
                    dto.setIsGroup(employeeDTO.getIsGroup());
                }
            }
        }

        return dtoList;
    }
}
