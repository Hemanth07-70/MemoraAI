package com.memoraai.processing.controller;

import com.memoraai.processing.dto.ProcessingJobResponse;
import com.memoraai.processing.entity.JobStatus;
import com.memoraai.processing.entity.ProcessingJob;
import com.memoraai.processing.mapper.ProcessingJobMapper;
import com.memoraai.processing.service.ProcessingJobService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import com.memoraai.auth.util.JwtUtil;
import com.memoraai.user.repository.UserRepository;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProcessingJobController.class)
@AutoConfigureMockMvc(addFilters = false)
class ProcessingJobControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProcessingJobService processingJobService;

    @MockBean
    private ProcessingJobMapper processingJobMapper;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private UserRepository userRepository;

    private UUID jobId;
    private ProcessingJob testJob;
    private ProcessingJobResponse jobResponse;

    @BeforeEach
    void setUp() {
        jobId = UUID.randomUUID();
        testJob = ProcessingJob.builder().id(jobId).build();
        jobResponse = ProcessingJobResponse.builder()
                .id(jobId)
                .status(JobStatus.PROCESSING)
                .progress(50)
                .build();
    }

    @Test
    void getJobById_success() throws Exception {
        when(processingJobService.getJobById(jobId)).thenReturn(testJob);
        when(processingJobMapper.toResponse(testJob)).thenReturn(jobResponse);

        mockMvc.perform(get("/api/v1/processing/jobs/" + jobId)
                .with(user("test@test.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(jobId.toString()))
                .andExpect(jsonPath("$.data.status").value("PROCESSING"))
                .andExpect(jsonPath("$.data.progress").value(50));
    }
}
