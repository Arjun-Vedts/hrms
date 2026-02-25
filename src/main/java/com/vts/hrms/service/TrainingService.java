package com.vts.hrms.service;

import com.vts.hrms.dto.*;
import com.vts.hrms.entity.*;
import com.vts.hrms.entity.Calendar;
import com.vts.hrms.entity.Program;
import com.vts.hrms.entity.Requisition;
import com.vts.hrms.exception.NotFoundException;
import com.vts.hrms.mapper.*;
import com.vts.hrms.repository.*;
import com.vts.hrms.util.FileStorageUtil;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
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

    private final MasterClientService masterClient;
    private final OrganizerRepository organizerRepository;
    private final AgencyMapper agencyMapper;
    private final CalendarMapper calenderMapper;
    private final CalenderRepository calenderRepository;
    private final ProgramMapper programMapper;
    private final ProgramRepository programRepository;
    private final RequisitionMapper requisitionMapper;
    private final RequisitionRepository requisitionRepository;
    private final FeedbackMapper feedbackMapper;
    private final FeedbackRepository feedbackRepository;

    public TrainingService(MasterClientService masterClient, OrganizerRepository organizerRepository, AgencyMapper agencyMapper, CalendarMapper calenderMapper, CalenderRepository calenderRepository, ProgramMapper programMapper, ProgramRepository programRepository, RequisitionMapper requisitionMapper, RequisitionRepository requisitionRepository, FeedbackMapper feedbackMapper,FeedbackRepository feedbackRepository) {
        this.masterClient = masterClient;
        this.organizerRepository = organizerRepository;
        this.agencyMapper = agencyMapper;
        this.calenderMapper = calenderMapper;
        this.calenderRepository = calenderRepository;
        this.programMapper = programMapper;
        this.programRepository = programRepository;
        this.requisitionMapper = requisitionMapper;
        this.requisitionRepository = requisitionRepository;
        this.feedbackMapper = feedbackMapper;
        this.feedbackRepository = feedbackRepository;
    }

    @Transactional(readOnly = true)
    public List<OrganizerIdDTO> getAllAgencies() {
        List<Organizer> list = organizerRepository.findAllByIsActive(1);
        return list.stream().map(agencyMapper::toDto).collect(Collectors.toCollection(LinkedList::new));
    }

    @Transactional
    public CalendarDTO addCalenderData(CalendarDTO dto, String username) throws IOException {
        log.info("Request to save calender for year {} by {}", dto.getYear(), username);
        Calendar calender = calenderMapper.toEntity(dto);
        calender.setCreatedBy(username);
        calender.setCreatedDate(LocalDateTime.now());
        calender.setIsActive(1);

        if (dto.getFile() != null && !dto.getFile().isEmpty()) {
            Path fullpath = Paths.get(appStorage, "Calendar", dto.getYear());
            calender.setFileName(dto.getFile().getOriginalFilename());
            FileStorageUtil.saveFile(fullpath, dto.getFile().getOriginalFilename(), dto.getFile());
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
            Organizer organizer = agencyMap.get(dto.getAgencyId());
            if (organizer != null) {
                dto.setAgency(organizer.getOrganizer());
            }
        });
        return dtoList;
    }

    public Optional<CalendarDTO> getCalendarById(Long id, String username) {
        return calenderRepository.findById(id).map(calenderMapper::toDto);
    }

    @Transactional
    public ProgramDTO addProgramData(@Valid ProgramDTO dto, String username) {
        log.info("Request to add program {} by {}", dto.getProgramName(), username);
        Program program = programMapper.toEntity(dto);
        program.setCreatedBy(username);
        program.setCreatedDate(LocalDateTime.now());
        program.setIsActive(1);
        program = programRepository.save(program);

        Organizer organizer = organizerRepository.findById(program.getOrganizerId())
                .orElseThrow(() -> new NotFoundException("Organizer data not found"));

        ProgramDTO programDTO = programMapper.toDto(program);
        programDTO.setOrganizer(organizer.getOrganizer());

        return programDTO;
    }

    @Transactional(readOnly = true)
    public List<ProgramDTO> getProgramList(String username) {
        log.info("Program list fetched by {}", username);
        List<Organizer> organizerList = organizerRepository.findAllByIsActive(1);
        List<Program> programList = programRepository.findAllByIsActive(1);

        Map<Long, Organizer> organizerMap = organizerList.stream()
                .collect(Collectors.toMap(Organizer::getOrganizerId, Function.identity()));

        List<ProgramDTO> dtoList = programMapper.toDto(programList);

        dtoList.forEach(dto -> {
            Organizer organizer = organizerMap.get(dto.getOrganizerId());
            if (organizer != null) {
                dto.setOrganizer(organizer.getOrganizer());
            }
        });
        return dtoList;
    }

    @Transactional
    public RequisitionDTO addRequisitionData(@Valid RequisitionDTO dto, String username) throws IOException {
        log.info("Request to add requisition for program {} by {}", dto.getProgramName(), username);

        Requisition requisition = requisitionMapper.toEntity(dto);
        requisition.setStatus("AA");
        requisition.setCreatedBy(username);
        requisition.setCreatedDate(LocalDateTime.now());
        requisition.setIsActive(1);

        Path fullpath = Paths.get(appStorage, "Requisition");
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

        requisition = requisitionRepository.save(requisition);
        return requisitionMapper.toDto(requisition);
    }

    @Transactional(readOnly = true)
    public List<RequisitionDTO> getRequisitionList(String username) {
        log.info("Requisition list fetched by {}", username);
        List<Organizer> organizerList = organizerRepository.findAllByIsActive(1);
        List<Program> programList = programRepository.findAllByIsActive(1);
        List<Requisition> list = requisitionRepository.findAllByIsActive(1);

        List<EmployeeDTO> employeeList = masterClient.getEmployeeList(xApiKey);

        Map<Long, EmployeeDTO> employeeMap = employeeList.stream()
                .collect(Collectors.toMap(EmployeeDTO::getEmpId, emp -> emp));

        Map<Long, Organizer> organizerMap = organizerList.stream()
                .collect(Collectors.toMap(Organizer::getOrganizerId, Function.identity()));

        Map<Long, Program> programMap = programList.stream()
                .collect(Collectors.toMap(Program::getProgramId, Function.identity()));

        List<RequisitionDTO> dtoList = requisitionMapper.toDto(list);

        dtoList.forEach(dto -> {
            EmployeeDTO employeeDTO = employeeMap.get(dto.getInitiatingOfficer());
            Program program = programMap.get(dto.getProgramId());
            Organizer organizer = organizerMap.get(program.getOrganizerId());
            dto.setProgramName(program.getProgramName());
            dto.setVenue(program.getVenue());
            dto.setRegistrationFee(program.getRegistrationFee());
            if (organizer != null) {
                dto.setOrganizer(organizer.getOrganizer());
                dto.setOrganizerContactName(organizer.getContactName());
                dto.setOrganizerPhoneNo(organizer.getPhoneNo());
                dto.setOrganizerFaxNo(organizer.getFaxNo());
                dto.setOrganizerEmail(organizer.getEmail());
            }
            if (employeeDTO != null) {
                dto.setEmpNo(employeeDTO.getEmpNo());
                dto.setInitiatingOfficerName((employeeDTO.getTitle() != null ? employeeDTO.getTitle() : "") + " " + employeeDTO.getEmpName());
                dto.setEmpDesigName(employeeDTO.getEmpDesigName());
                dto.setEmpDivCode(employeeDTO.getEmpDivCode());
                dto.setEmail(employeeDTO.getEmail());
                dto.setMobileNo(employeeDTO.getMobileNo());
            }
        });
        return dtoList;
    }

    @Transactional
    public RequisitionDTO getRequisitionById(Long id, String username) {
        log.info("Request to fetch Requisition data for id {} by {}", id, username);
        if (id == null) {
            throw new NotFoundException("Requisition id cannot be null");
        }
        List<Organizer> organizerList = organizerRepository.findAllByIsActive(1);
        List<Program> programList = programRepository.findAllByIsActive(1);

        Map<Long, Organizer> organizerMap = organizerList.stream()
                .collect(Collectors.toMap(Organizer::getOrganizerId, Function.identity()));

        Map<Long, Program> programMap = programList.stream()
                .collect(Collectors.toMap(Program::getProgramId, Function.identity()));

        Requisition requisition = requisitionRepository.findById(id).orElseThrow(() -> new NotFoundException("Requisition not found"));

        Program program = programMap.get(requisition.getProgramId());
        Organizer org = organizerMap.get(program.getOrganizerId());

        RequisitionDTO requisitionDTO = requisitionMapper.toDto(requisition);
        requisitionDTO.setOrganizer(org.getOrganizer());
        requisitionDTO.setOrganizerId(org.getOrganizerId());
        requisitionDTO.setProgramName(program.getProgramName());
        requisitionDTO.setVenue(program.getVenue());
        requisitionDTO.setRegistrationFee(program.getRegistrationFee());
        return requisitionDTO;
    }

    @Transactional
    public Optional<RequisitionDTO> updateRequisitionData(@Valid RequisitionDTO dto, String username) {

        log.info("Request to update requisition for id {} by {}", dto.getRequisitionId(), username);

        Path fullpath = Paths.get(appStorage, "Requisition");

        return requisitionRepository
                .findById(dto.getRequisitionId())
                .map(existingReq -> {
                    existingReq.setModifiedBy(username);
                    existingReq.setModifiedDate(LocalDateTime.now());

                    // ECS
                    updateFile(dto.getMultipartFileEcs(), existingReq.getFileEcs(), fullpath,existingReq::setFileEcs);
                    // Cheque
                    updateFile(dto.getMultipartFileCheque(), existingReq.getFileCheque(), fullpath, existingReq::setFileCheque);
                    // PAN
                    updateFile(dto.getMultipartFilePan(), existingReq.getFilePan(), fullpath, existingReq::setFilePan);
                    // Brochure
                    updateFile(dto.getMultipartFileBrochure(), existingReq.getFileBrochure(), fullpath, existingReq::setFileBrochure);

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


    public FeedbackDTO requisitionFeedback(@Valid FeedbackDTO dto, String username) {
        log.info("Request to requisition feedback requisitionId {} by {}", dto.getRequisitionId(),username);
        Feedback feedback = feedbackMapper.toEntity(dto);
        feedback.setCreatedBy(username);
        feedback.setCreatedDate(LocalDateTime.now());
        feedback.setIsActive(1);
        feedback = feedbackRepository.save(feedback);


        FeedbackDTO feedbackDTO = feedbackMapper.toDto(feedback);

        return feedbackDTO;
    }

    public List<FeedbackDTO> getFeedbackList(String username) {
        log.info("Feedback list fetched by {}", username);
        List<Feedback> feedbackList = feedbackRepository.findByIsActive(1);

        if (feedbackList == null || feedbackList.isEmpty()) {
            return Collections.emptyList();
        }

        List<FeedbackDTO> feedbbackdto = feedbackMapper.toDto(feedbackList);

        if (feedbbackdto == null || feedbbackdto.isEmpty()) {
            return Collections.emptyList();
        }

        List<EmployeeDTO> employeeList = masterClient.getEmployeeList(xApiKey);

        Map<Long, EmployeeDTO> employeeMap = employeeList != null
                ? employeeList.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(EmployeeDTO::getEmpId, emp -> emp))
                : Collections.emptyMap();

        feedbbackdto.forEach(d -> {

            if (d == null) return;

            EmployeeDTO employeeDTO = employeeMap.get(d.getParticipantId());

            if (employeeDTO != null) {

                String title = employeeDTO.getTitle() != null ? employeeDTO.getTitle() : "";
                String empName = employeeDTO.getEmpName() != null ? employeeDTO.getEmpName() : "";
                String designation = employeeDTO.getEmpDesigName() != null ? employeeDTO.getEmpDesigName() : "";

                String participantName =
                        (title.isEmpty() ? "" : title + " ")
                                + empName
                                + (designation.isEmpty() ? "" : ", " + designation);

                d.setParticipantName(participantName.trim());
                d.setDivisionName(employeeDTO.getEmpDivCode());
            }

            RequisitionDTO requisitionDto = null;

            if (d.getRequisitionId() != null) {
                requisitionDto = getRequisitionById(d.getRequisitionId(), username);
            }

            if (requisitionDto != null) {
                d.setProgramName(requisitionDto.getProgramName());
                d.setOrganizer(requisitionDto.getOrganizer());
                d.setFromDate(requisitionDto.getFromDate());
                d.setToDate(requisitionDto.getToDate());
                d.setProgramDuration(requisitionDto.getDuration());
            }

        });

        return feedbbackdto;
    }
}
