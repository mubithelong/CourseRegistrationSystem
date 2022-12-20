package com.example.courseregistrationsystem.service.impl;

import com.example.courseregistrationsystem.domain.Registration;
import com.example.courseregistrationsystem.domain.RegistrationEvent;
import com.example.courseregistrationsystem.domain.RegistrationRequest;
import com.example.courseregistrationsystem.domain.Student;
import com.example.courseregistrationsystem.repo.RegistrationRepository;
import com.example.courseregistrationsystem.repo.StudentRepository;
import com.example.courseregistrationsystem.service.dto.RegistrationEventDto;
import com.example.courseregistrationsystem.repo.RegistrationEventRepository;
import com.example.courseregistrationsystem.service.RegistrationEventService;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.List;
import java.util.Optional;

@Service
public class RegistrationEventServiceImpl implements RegistrationEventService {
    @Autowired
    private RegistrationEventRepository registrationEventRepository;
    @Autowired
    private StudentRepository studentRepository;
    @Autowired
    private RegistrationRepository registrationRepository;

    @Autowired
    ModelMapper modelMapper;

    @Override
    public RegistrationEventDto createRegistrationEvent(RegistrationEventDto registrationEventDto) {
        System.out.println("---------service");
        RegistrationEvent registrationEvent = modelMapper.map(registrationEventDto, RegistrationEvent.class);
        System.out.println(registrationEventDto.getRegistrationGroupDtos().get(0).getTrack());
        return modelMapper.map(registrationEventRepository.save(registrationEvent), RegistrationEventDto.class);
    }

    @Override
    public List<RegistrationEventDto> getAllRegistrationEvents() {
        List<RegistrationEvent> registrationEvents = registrationEventRepository.findAll();
        List<RegistrationEventDto> registrationEventDtos =
                registrationEvents.stream().map(registrationEvent -> modelMapper.map(registrationEvent, RegistrationEventDto.class)).toList();
        return registrationEventDtos;

    }

    @Override
    public RegistrationEventDto updateRegistrationEvent(RegistrationEventDto registrationEventDto) {
        RegistrationEvent registrationEvent = registrationEventRepository.save(modelMapper.map(registrationEventDto, RegistrationEvent.class));
        return modelMapper.map(registrationEvent, RegistrationEventDto.class);
    }

    @Override
    public void deleteRegistrationEvent(RegistrationEventDto registrationEventDto) {
        registrationEventRepository.delete(modelMapper.map(registrationEventDto, RegistrationEvent.class));
    }

    @Override
    @Transactional
    public void process(int id) {
        List<Student> students =
                studentRepository
                        .findAllByRegistrationGroupRegistrationEvent_IdOrderByRegistrationRequestsAsc(id);

        for (Student student : students) {
            for (RegistrationRequest registrationRequest : student.getRegistrationRequests()) {
                // search in registration for registration request course offering
                Optional<Registration> registration = null;
                if (student.getRegistrations() != null) {
                     registration = student.getRegistrations().stream()
                            .filter(r ->
                                    r.getCourseOffering().equals(registrationRequest.getCourseOffering()))
                            .findFirst();
                }
                // search in registration for registration request course offering
                Optional<Registration> registrationCurrentRequestBlock = null;
                if (student.getRegistrations() != null) {
                    registrationCurrentRequestBlock = student.getRegistrations().stream()
                            .filter(r ->
                                    r.getCourseOffering().getAcademicBlock().equals(registrationRequest.getCourseOffering().getAcademicBlock()))
                            .findFirst();
                }
                // if the student does not have course in registration
                // And the student does not have course in block
                if ((registration == null || !registration.isPresent()) &&
                        (registrationCurrentRequestBlock == null || !registrationCurrentRequestBlock.isPresent())) {
                    // if there is available seats
                    if (registrationRequest.getCourseOffering().getCapacity()
                            - registrationRequest.getCourseOffering().getRegistrations().size() > 0) {
                        // register the student for this course
                        Registration newRegistration = Registration.builder()
                                .student(student)
                                .courseOffering(registrationRequest.getCourseOffering())
                                .build();

                        registrationRepository.save(newRegistration);
                    }
                }
            }
        }
    }

}
