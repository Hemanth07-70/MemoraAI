package com.memoraai.document.mapper;

import com.memoraai.document.dto.DocumentResponse;
import com.memoraai.document.entity.Document;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;

@Mapper(componentModel = "spring")
public interface DocumentMapper {

    @Mapping(target = "downloadUrl", source = "id", qualifiedByName = "generateDownloadUrl")
    DocumentResponse toResponse(Document document);

    List<DocumentResponse> toResponseList(List<Document> documents);

    @Named("generateDownloadUrl")
    default String generateDownloadUrl(java.util.UUID id) {
        if (id == null) {
            return null;
        }
        return "/api/v1/documents/" + id + "/download";
    }
}
