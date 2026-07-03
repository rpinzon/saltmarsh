package com.saltmarsh.service;

import com.saltmarsh.domain.UserAccount;
import com.saltmarsh.domain.Vessel;
import com.saltmarsh.domain.enums.Role;
import com.saltmarsh.domain.enums.VesselType;
import com.saltmarsh.dto.VesselRequest;
import com.saltmarsh.exception.BusinessException;
import com.saltmarsh.exception.ForbiddenException;
import com.saltmarsh.exception.NotFoundException;
import com.saltmarsh.repository.UserAccountRepository;
import com.saltmarsh.repository.VesselRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class VesselService {

    private final VesselRepository vesselRepository;
    private final UserAccountRepository userAccountRepository;
    private final AuditService auditService;

    public VesselService(VesselRepository vesselRepository,
                         UserAccountRepository userAccountRepository,
                         AuditService auditService) {
        this.vesselRepository = vesselRepository;
        this.userAccountRepository = userAccountRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<Vessel> listFor(UserAccount actor) {
        if (actor.getRole().isStaffOrAbove()) {
            return vesselRepository.findByActiveTrueOrderByNameAsc();
        }
        return vesselRepository.findByOwnerIdAndActiveTrueOrderByNameAsc(actor.getId());
    }

    @Transactional(readOnly = true)
    public Vessel getVisible(Long id, UserAccount actor) {
        Vessel vessel = vesselRepository.findByIdWithOwner(id)
                .orElseThrow(() -> new NotFoundException("Vessel not found"));
        assertCanView(vessel, actor);
        return vessel;
    }

    @Transactional
    public Vessel register(VesselRequest request, UserAccount actor) {
        if (vesselRepository.existsByRegistrationNumberIgnoreCase(request.registrationNumber().trim())) {
            throw new BusinessException("DUPLICATE_REGISTRATION", "Registration number already exists");
        }

        UserAccount owner = resolveOwner(request.ownerId(), actor);

        Vessel vessel = new Vessel();
        vessel.setOwner(owner);
        vessel.setName(request.name().trim());
        vessel.setRegistrationNumber(request.registrationNumber().trim().toUpperCase());
        vessel.setLengthFeet(request.lengthFeet());
        vessel.setBeamFeet(request.beamFeet());
        vessel.setDraftFeet(request.draftFeet());
        vessel.setVesselType(VesselType.valueOf(request.vesselType()));
        vessel.setActive(true);

        Vessel saved = vesselRepository.save(vessel);
        auditService.record(actor, "VESSEL_REGISTERED", "Vessel", saved.getId(),
                "Registered " + saved.getName() + " (" + saved.getRegistrationNumber() + ")");
        return saved;
    }

    @Transactional
    public Vessel update(Long id, VesselRequest request, UserAccount actor) {
        Vessel vessel = getVisible(id, actor);
        if (!actor.getRole().isStaffOrAbove() && !vessel.getOwner().getId().equals(actor.getId())) {
            throw new ForbiddenException("You may only edit your own vessels");
        }

        String newReg = request.registrationNumber().trim().toUpperCase();
        if (!vessel.getRegistrationNumber().equalsIgnoreCase(newReg)
                && vesselRepository.existsByRegistrationNumberIgnoreCase(newReg)) {
            throw new BusinessException("DUPLICATE_REGISTRATION", "Registration number already exists");
        }

        if (actor.getRole().isStaffOrAbove() && request.ownerId() != null
                && !request.ownerId().equals(vessel.getOwner().getId())) {
            vessel.setOwner(userAccountRepository.findById(request.ownerId())
                    .orElseThrow(() -> new NotFoundException("Owner not found")));
        }

        vessel.setName(request.name().trim());
        vessel.setRegistrationNumber(newReg);
        vessel.setLengthFeet(request.lengthFeet());
        vessel.setBeamFeet(request.beamFeet());
        vessel.setDraftFeet(request.draftFeet());
        vessel.setVesselType(VesselType.valueOf(request.vesselType()));

        auditService.record(actor, "VESSEL_UPDATED", "Vessel", vessel.getId(), "Updated vessel details");
        return vessel;
    }

    @Transactional
    public void deactivate(Long id, UserAccount actor) {
        Vessel vessel = getVisible(id, actor);
        if (!actor.getRole().isStaffOrAbove() && !vessel.getOwner().getId().equals(actor.getId())) {
            throw new ForbiddenException("You may only deactivate your own vessels");
        }
        vessel.setActive(false);
        auditService.record(actor, "VESSEL_DEACTIVATED", "Vessel", vessel.getId(), "Vessel deactivated");
    }

    private UserAccount resolveOwner(Long ownerId, UserAccount actor) {
        if (actor.getRole() == Role.BOATER) {
            return actor;
        }
        if (ownerId == null) {
            return actor;
        }
        return userAccountRepository.findById(ownerId)
                .orElseThrow(() -> new NotFoundException("Owner not found"));
    }

    private void assertCanView(Vessel vessel, UserAccount actor) {
        if (actor.getRole().isStaffOrAbove()) {
            return;
        }
        if (!vessel.getOwner().getId().equals(actor.getId())) {
            throw new ForbiddenException("You cannot view this vessel");
        }
    }
}
