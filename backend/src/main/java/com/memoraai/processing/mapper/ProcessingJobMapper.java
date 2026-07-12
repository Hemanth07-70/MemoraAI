package com.memoraai.processing.mapper;

import com.memoraai.processing.dto.ProcessingJobResponse;
import com.memoraai.processing.entity.ProcessingJob;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ProcessingJobMapper {

    @Mapping(target = "documentId", source = "document.id")
    ProcessingJobResponse toResponse(ProcessingJob job);

    List<ProcessingJobResponse> toResponseList(List<ProcessingJob> jobs);
}
