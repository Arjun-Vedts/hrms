package com.vts.hrms.service;

import com.vts.hrms.dto.*;
import com.vts.hrms.entity.SignAuthRole;
import com.vts.hrms.entity.SignRoleAuthority;
import com.vts.hrms.mapper.SignAuthRoleMapper;
import com.vts.hrms.mapper.SignRoleAuthorityMapper;
import com.vts.hrms.repository.LoginRepository;
import com.vts.hrms.repository.SignAuthRoleRepository;
import com.vts.hrms.repository.SignRoleAuthorityRepository;
import com.vts.hrms.util.CommonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class MasterService {

    private static final Logger log = LoggerFactory.getLogger(MasterService.class);

    @Value("${x_api_key}")
    private String xApiKey;

    @Value("${labCode}")
    private String labCode;

    private final MasterClientService masterClient;
    private final LoginRepository loginRepository;
    private final SignAuthRoleRepository signAuthRoleRepository;
    private final SignRoleAuthorityRepository signRoleAuthorityRepository;
    private final SignAuthRoleMapper signAuthRoleMapper;
    private final SignRoleAuthorityMapper signRoleAuthorityMapper;
    private final MasterCacheService masterCacheService;

    public MasterService(MasterClientService masterClient, LoginRepository loginRepository, SignAuthRoleRepository signAuthRoleRepository, SignRoleAuthorityRepository signRoleAuthorityRepository, SignAuthRoleMapper signAuthRoleMapper, SignRoleAuthorityMapper signRoleAuthorityMapper, MasterCacheService masterCacheService) {
        this.masterClient = masterClient;
        this.loginRepository = loginRepository;
        this.signAuthRoleRepository = signAuthRoleRepository;
        this.signRoleAuthorityRepository = signRoleAuthorityRepository;
        this.signAuthRoleMapper = signAuthRoleMapper;
        this.signRoleAuthorityMapper = signRoleAuthorityMapper;
        this.masterCacheService = masterCacheService;
    }

    @Cacheable(value = "designationList")
    public List<DesignationDTO> getEmpDesigMaster() {
        log.info("Fetching designation master");
        return masterClient.getEmpDesigMaster(xApiKey);
    }

    @Cacheable(value = "divisionList")
    public List<DivisionDTO> getDivisionMaster() {
        log.info("Fetching division master");
        return masterClient.getDivisionMaster(xApiKey);
    }

    public LoginEmployeeDto getEmployeeByUsername(String username) {
        log.info("Fetching employee with username {}", username);

        LoginEmployeeDto dto = loginRepository.findByUserName(username);

        List<EmployeeDTO> employeeList = masterClient.getEmployeeMasterList(xApiKey);

        Map<Long, EmployeeDTO> employeeMap = employeeList.stream()
                .filter(e -> labCode != null && labCode.equalsIgnoreCase(e.getLabCode()))
                .collect(Collectors.toMap(EmployeeDTO::getEmpId, emp -> emp));

        EmployeeDTO employee = employeeMap.get(dto.getEmpId());

        dto.setEmpId(dto.getEmpId());
        dto.setEmpNo(employee.getEmpNo());
        dto.setEmployeeType(employee.getEmployeeType());
        dto.setTitle(employee.getTitle());
        dto.setHindiTitle(employee.getHindiTitle());
        dto.setSalutation(employee.getSalutation());
        dto.setHindiSalutation(employee.getHindiSalutation());
        dto.setEmpName(employee.getEmpName());
        dto.setHindiEmpName(employee.getHindiEmpName());
        dto.setEmpDesigName(employee.getEmpDesigName());
        dto.setHindiEmpDesigName(employee.getHindiDesignation());
        dto.setEmpStatus(employee.getEmpStatus());
        dto.setRoleName(dto.getRoleName());
        dto.setHindiRoleName(dto.getHindiRoleName());
        dto.setDivisionId(employee.getDivisionId());
        dto.setLoginId(dto.getLoginId());
        dto.setRoleId(dto.getRoleId());
        dto.setIsGroup(employee.getIsGroup());

        return dto;
    }

    @Cacheable(value = "employeeList")
    public List<EmployeeDTO> getEmployeeList() {
        log.info("Fetching employee master");
        return masterClient.getEmployeeMasterList(xApiKey).stream()
                .filter(e -> labCode != null && labCode.equalsIgnoreCase(e.getLabCode()))
                .peek(emp-> emp.setEmpName(CommonUtil.buildEmployeeName(emp,false)))
                .toList();

    }

    @Cacheable(value = "signAuthRoles", key = "#username")
    @Transactional(readOnly = true)
    public List<SignAuthRoleDTO> getSignAuthRoles(String username) {
        log.info("Request to get all SignAuthRoles by username {}", username);
        return signAuthRoleRepository
                .findAllByIsActive(1)
                .stream()
                .map(signAuthRoleMapper::toDto)
                .collect(Collectors.toCollection(LinkedList::new));
    }

    @Cacheable(value = "signAuthorities", key = "#username")
    @Transactional(readOnly = true)
    public List<SignRoleAuthorityDTO> getSignAuthorities(String username) {
        log.info("Request to get all sign role authority list by username {}", username);
        List<SignRoleAuthorityDTO> authorityDTOList = signRoleAuthorityRepository
                .findAllByIsActive(1)
                .stream()
                .map(signRoleAuthorityMapper::toDto)
                .collect(Collectors.toList());

        if (authorityDTOList.isEmpty()) {
            return authorityDTOList;
        }

        List<SignAuthRole> authRoleList = signAuthRoleRepository.findAll();
        Map<Long, SignAuthRole> signRoleMap = authRoleList.stream()
                .collect(Collectors.toMap(SignAuthRole::getSignAuthRoleId, Function.identity()));

        Map<Long, EmployeeDTO> employeeMap = masterCacheService.getLongEmployeeDTOMap();

        for (SignRoleAuthorityDTO dto : authorityDTOList) {

            SignAuthRole role = signRoleMap.get(dto.getSignAuthRoleId());
            if (role != null) {
                dto.setSignAuthRoleDesc(role.getSignAuthRole());
            }

            EmployeeDTO employee = employeeMap.get(dto.getEmpId());
            if (employee != null) {
                dto.setEmployeeName(CommonUtil.buildEmployeeName(employee,true));
                dto.setEmployeeDesignation(employee.getEmpDesigName());
            }
        }
        return authorityDTOList;
    }

    @CacheEvict(value="signAuthorities", allEntries=true)
    public SignRoleAuthorityDTO addSignRoleAuthority(SignRoleAuthorityDTO dto, String username) {
        SignRoleAuthority roleAuthority = signRoleAuthorityMapper.toEntity(dto);
        roleAuthority.setCreatedBy(username);
        roleAuthority.setSerialNo(roleAuthority.getSerialNo()!=null? roleAuthority.getSerialNo() : 1L);
        roleAuthority.setCreatedDate(LocalDateTime.now());
        roleAuthority.setIsActive(1);
        roleAuthority = signRoleAuthorityRepository.save(roleAuthority);
        return signRoleAuthorityMapper.toDto(roleAuthority);
    }

    @CacheEvict(value="signAuthorities", allEntries=true)
    public Optional<SignRoleAuthorityDTO> updateSignRoleAuthority(SignRoleAuthorityDTO dto, String username) {
        log.info("Request to update sign role authority for id {} by {}", dto.getSignRoleAuthorityId(), username);
        return signRoleAuthorityRepository
                .findById(dto.getSignRoleAuthorityId())
                .map(existingSign -> {
                    existingSign.setModifiedBy(username);
                    existingSign.setModifiedDate(LocalDateTime.now());
                    signRoleAuthorityMapper.partialUpdate(existingSign, dto);
                    return existingSign;
                })
                .map(signRoleAuthorityRepository::save)
                .map(signRoleAuthorityMapper::toDto);
    }

    public List<ProjectMasterDTO> getProjectMasterList(String username) {
        log.info("Request get project list by {} ", username);
        return masterClient.getProjectMasterList(xApiKey);
    }

    public List<ProjectEmployeeDto> getProjectListByEmpId(Long empid, String username){
        log.info("Request getProjectListById {} ", username);
        return masterClient.getProjectListByEmpId(xApiKey, empid);
    }

    public List<RoleMaster> getRoleMasterListed(String username,String token) {
        log.info("Request get roleMaster list by {} ", username);
        return masterClient.getRoleMasterList(token);
    }

    public ResponseEntity<String> addProjectsRolesIds(String username, ProjectAssignEmpDto dto, String token) {
        log.info("Request addProjectsRolesIds {} ", username);

        return masterClient.addProjectRoleIds(dto,token,username);
    }
}
