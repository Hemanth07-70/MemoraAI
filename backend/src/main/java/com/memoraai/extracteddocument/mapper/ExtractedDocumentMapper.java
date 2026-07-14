package com.memoraai.extracteddocument.mapper;

import com.memoraai.extracteddocument.dto.ExtractedDocumentResponse;
import com.memoraai.extracteddocument.entity.ExtractedDocument;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ExtractedDocumentMapper {

    @Mapping(source = "document.id", target = "documentId")
    ExtractedDocumentResponse toResponse(ExtractedDocument extractedDocument);
}
