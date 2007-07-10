package org.mifos.framework.components.audit.persistence;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.HibernateException;
import org.mifos.application.NamedQueryConstants;
import org.mifos.application.master.business.CustomFieldDefinitionEntity;
import org.mifos.application.master.business.LookUpEntity;
import org.mifos.application.master.business.LookUpLabelEntity;
import org.mifos.application.master.business.LookUpValueLocaleEntity;
import org.mifos.application.meeting.business.RecurrenceTypeEntity;
import org.mifos.application.productdefinition.business.PrdStatusEntity;
import org.mifos.framework.exceptions.PersistenceException;
import org.mifos.framework.persistence.Persistence;

public class AuditConfigurationPersistence extends Persistence {

	public Map<String, String> retrieveAllCustomFieldsDefinition(Short localeId)
			throws PersistenceException {
		Map<String, String> valueMap = new HashMap<String, String>();
		try {
			List<CustomFieldDefinitionEntity> customFields = executeNamedQuery(
					NamedQueryConstants.RETRIEVE_ALL_CUSTOM_FIELDS, null);
			for (CustomFieldDefinitionEntity field : customFields) {
				LookUpEntity lookUpEntity = field
						.getLookUpEntity();
				Set<LookUpLabelEntity> lookUpLabelSet = lookUpEntity
						.getLookUpLabelSet();
				for (LookUpLabelEntity lookUpLabel : lookUpLabelSet) {
					if (lookUpLabel.getLocaleId().equals(localeId)) {
						valueMap.put(field.getFieldId()
								.toString(), lookUpLabel.getLabelName());
					}
				}
			}
		} catch (HibernateException e) {
			throw new PersistenceException(e);
		}
		return valueMap;
	}
	
	public Map<String, String> retrieveProductStatus(Short localeId) 
	throws PersistenceException{
		Map<String, String> valueMap = new HashMap<String, String>();
		try {
			List<PrdStatusEntity> productStates = executeNamedQuery(
					NamedQueryConstants.ALL_PRD_STATES, null);
			for (PrdStatusEntity prdStatusEntity : productStates) {
				Set<LookUpValueLocaleEntity> lookUpValueLocaleSet = 
					prdStatusEntity.getPrdState()
					    .getLookUpValue().getLookUpValueLocales();
				for (LookUpValueLocaleEntity lookup : lookUpValueLocaleSet) {
					if (lookup.getLocaleId().equals(localeId)) {
						valueMap.put(
							prdStatusEntity.getOfferingStatusId().toString(), 
							lookup.getLookUpValue());
					}
				}
			}
		} catch (HibernateException e) {
			throw new PersistenceException(e);
		}
		return valueMap;
	}
	
	public Map<String, String> retrieveRecurrenceTypes(Short localeId) 
	throws PersistenceException {
		Map<String, String> valueMap = new HashMap<String, String>();
		try {
			List<RecurrenceTypeEntity> recurrenceTypes = executeNamedQuery(
					NamedQueryConstants.FETCH_ALL_RECURRENCE_TYPES, null);
			for (RecurrenceTypeEntity recurrence : recurrenceTypes) {
				valueMap.put(
					recurrence.getRecurrenceId().toString(), 
					recurrence.getRecurrenceName());
			}
		} catch (HibernateException e) {
			throw new PersistenceException(e);
		}
		return valueMap;
	}
	
}
