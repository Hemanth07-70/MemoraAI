package com.memoraai.anme.mapper;

import com.memoraai.anme.dto.ConceptDto;
import com.memoraai.anme.dto.ConceptRelationshipDto;
import com.memoraai.anme.dto.UserMemoryStateDto;
import com.memoraai.anme.entity.Concept;
import com.memoraai.anme.entity.ConceptRelationship;
import com.memoraai.anme.entity.UserMemoryState;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ANMEMapper {

    @Mapping(source = "document.id", target = "documentId")
    ConceptDto conceptToDto(Concept concept);

    @Mapping(source = "user.id", target = "userId")
    UserMemoryStateDto userMemoryStateToDto(UserMemoryState state);

    @Mapping(source = "sourceConcept.id", target = "sourceConceptId")
    @Mapping(source = "sourceConcept.name", target = "sourceConceptName")
    @Mapping(source = "targetConcept.id", target = "targetConceptId")
    @Mapping(source = "targetConcept.name", target = "targetConceptName")
    ConceptRelationshipDto relationshipToDto(ConceptRelationship relationship);
}
