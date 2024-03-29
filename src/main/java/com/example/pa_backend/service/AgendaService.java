package com.example.pa_backend.service;

import com.example.pa_backend.dto.AgendaDTO;
import com.example.pa_backend.dto.AgendaResponseDTO;
import com.example.pa_backend.entity.Agenda;
import com.example.pa_backend.entity.TherapyConfiguration;
import com.example.pa_backend.entity.user.Therapist;
import com.example.pa_backend.exception.ServiceException;
import com.example.pa_backend.repository.AgendaRepository;
import com.example.pa_backend.repository.TherapistRepository;
import com.example.pa_backend.repository.TherapyConfigurationRepository;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class AgendaService {

    @Autowired
    private AgendaRepository agendaRepository;

    @Autowired
    private TherapyConfigurationRepository therapyConfigurationRepository;

    @Autowired
    private TherapistRepository therapistRepository;


    public List<Agenda> getAll() {

        List<Agenda> agenda = agendaRepository.findAll();
        if (agenda.isEmpty()) {
            throw new RuntimeException("No content");
        }
        return agenda;

    }


    public Agenda getById(int id) {
        Agenda agenda = agendaRepository.findById(id).orElseThrow(() -> new RuntimeException("Not found"));
        return agenda;
    }


    public Agenda create(AgendaDTO agendaDTO) {
        Agenda  agenda =  new Agenda();

        agenda.setTherapistId(agendaDTO.getTherapistId());
        agenda.setClientEmail(agendaDTO.getClientEmail());
        agenda.setClientName(agendaDTO.getClientName());
        agenda.setAddress(agendaDTO.getAddress());
        agenda.setDate(agendaDTO.getDate());
        agenda.setStartTime(agendaDTO.getTime());
        LocalTime endTime = agendaDTO.getTime().plusHours(1).plusMinutes(30);
        agenda.setEndTime(endTime);

        List<Agenda> agendas =
                agendaRepository.findAllByTherapistIdAndDateAndStartTimeGreaterThanEqualAndEndTimeLessThanEqualAndClientEmail(
                        agenda.getTherapistId(),
                        agendaDTO.getDate(),
                        agenda.getStartTime(),
                        agenda.getEndTime(),
                        agenda.getClientEmail()
                );
    if(!agendas.isEmpty()){
        throw  new ServiceException("Horário não disponível", HttpStatus.BAD_REQUEST);
    }

        return agendaRepository.save(agenda);
    }

    private Agenda mapAgenda(AgendaDTO agendaDTO) {
        ModelMapper modelMapper = new ModelMapper();
        modelMapper
                .getConfiguration()
                .setMatchingStrategy(MatchingStrategies.STRICT)
                .setSkipNullEnabled(true);
        Agenda map = modelMapper.map(agendaDTO, Agenda.class);
        return map;
    }

    private LocalTime getEndTime(LocalTime startTime, TherapyConfiguration therapyConfiguration) {
        Duration between = Duration.between(therapyConfiguration.getStartTime(), therapyConfiguration.getEndTime());
        return startTime.plus(between);

    }

    public Agenda update(int id, AgendaDTO agendaDTO) {
        Agenda byId = getById(id);

        ModelMapper modelMapper = new ModelMapper();
        modelMapper
                .getConfiguration()
                .setMatchingStrategy(MatchingStrategies.STRICT)
                .setSkipNullEnabled(true);
        modelMapper.map(agendaDTO, byId);

        Agenda save = agendaRepository.save(byId);
        return save;

    }


    public Object delete(int id) {
        Agenda byId = getById(id);
        try {
            agendaRepository.delete(byId);
            return true;
        } catch (Exception e) {
            throw new RuntimeException("Error on delete");
        }
    }


    public List<AgendaResponseDTO> getByEmail(String email) {
        List<Agenda> allByClientEmail = agendaRepository.findAllByClientEmail(email);
        List<AgendaResponseDTO> agendaResponseDTOS = new ArrayList<>();
        for (Agenda agenda : allByClientEmail) {
            Therapist therapist = therapistRepository.findById(agenda.getTherapistId()).orElseThrow(
                    () -> new ServiceException("Not found", HttpStatus.BAD_REQUEST));

            AgendaResponseDTO agendaResponseDTO = new AgendaResponseDTO().setAddress(agenda.getAddress())
                                                                         .setClientEmail(agenda.getClientEmail())
                                                                         .setClientName(agenda.getClientName())
                                                                         .setDate(agenda.getDate())
                                                                         .setStartTime(agenda.getStartTime())
                                                                         .setEndTime(agenda.getEndTime())
                                                                         .setTherapistId(agenda.getTherapistId())
                                                                         .setThearapistName(therapist.getName())
                                                                         .setThearapistEmail(therapist.getEmail())
                    .setId(agenda.getId());

            agendaResponseDTOS.add(agendaResponseDTO);

        }

        return agendaResponseDTOS;

    }
}
