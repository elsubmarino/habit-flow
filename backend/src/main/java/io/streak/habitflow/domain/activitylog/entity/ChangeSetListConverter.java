package io.streak.habitflow.domain.activitylog.entity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.streak.habitflow.domain.activitylog.vo.ChangeSet;
import io.streak.habitflow.global.error.ErrorCode;
import io.streak.habitflow.global.error.exception.BusinessException;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.ArrayList;
import java.util.List;

@Converter
public class ChangeSetListConverter implements AttributeConverter<List<ChangeSet>, String> {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(List<ChangeSet> attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return "[]";
        }
        try{
            return objectMapper.writeValueAsString(attribute);
        }catch(JsonProcessingException e){
            throw new BusinessException(ErrorCode.DATA_CONVERSION_FAILED, "ChangeSet 직렬화 실패", e);
        }
    }

    @Override
    public List<ChangeSet> convertToEntityAttribute(String dbData) {
        if(dbData == null || dbData.isEmpty()){
            return new ArrayList<>();
        }
        try{
            return objectMapper.readValue(dbData, new TypeReference<List<ChangeSet>>(){});
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.DATA_CONVERSION_FAILED, "ChangeSet 역직렬화 실패", e);
        }
    }
}
