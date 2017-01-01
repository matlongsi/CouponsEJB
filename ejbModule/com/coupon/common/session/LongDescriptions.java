package com.coupon.common.session;

import java.util.logging.Logger;

import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Remote;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.persistence.EntityManager;
import javax.persistence.EntityNotFoundException;
import javax.persistence.PersistenceContext;

import com.coupon.common.entity.LongDescriptionEntity;
import com.coupon.common.bean.LongDescriptionBean;
import com.coupon.common.exception.ResourceNotFoundException;
import com.coupon.common.session.remote.LongDescriptionsRemote;


@Remote
@LocalBean
@Stateless(name=LongDescriptionsRemote.NAME, mappedName=LongDescriptionsRemote.MAPPED_NAME)
@TransactionManagement(TransactionManagementType.CONTAINER)
public class LongDescriptions implements LongDescriptionsRemote {

	@PersistenceContext(unitName="CouponsJPAService")
	private EntityManager manager;

	@EJB private MarketingMaterials mmEJB;
	
	static final Logger logger = Logger.getLogger(LongDescriptions.class.getName());

	public LongDescriptions() {
	}
	
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public LongDescriptionEntity findEntity(long ldId) {

		LongDescriptionEntity ldEnt = manager.find(LongDescriptionEntity.class, ldId);		
	  	if (ldEnt == null) {
	  		throw new ResourceNotFoundException(
	  				String.format("LongDescription resource with id '%d' not found", ldId));
	  	}

		return ldEnt;
	}

	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	@Override public LongDescriptionBean find(long ldId) {

		return new LongDescriptionBean().init(findEntity(ldId));
	}

	@TransactionAttribute(TransactionAttributeType.MANDATORY)
	public LongDescriptionEntity createEntity(LongDescriptionBean ld) {

		LongDescriptionEntity ldEnt = new LongDescriptionEntity();
		ldEnt.setLongDescription(ld.getLongDescription());

		return ldEnt;
	}

	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	@Override public LongDescriptionBean create(LongDescriptionBean ld) {

		LongDescriptionEntity ldEnt = createEntity(ld);
		ldEnt.setMarketingMaterial(mmEJB.findEntity(ld.getParentId()));
		manager.persist(ldEnt);
		manager.flush();

		return new LongDescriptionBean().init(ldEnt);
	}

	@TransactionAttribute(TransactionAttributeType.MANDATORY)
	public LongDescriptionEntity updateEntity(LongDescriptionBean ld) {

		LongDescriptionEntity ldEntRef;
		try {

			ldEntRef = manager.getReference(LongDescriptionEntity.class, ld.getId());
			ldEntRef.setLongDescription(ld.getLongDescription());
			manager.merge(ldEntRef);
			manager.flush();
		}
		catch (EntityNotFoundException ex) {
			throw new ResourceNotFoundException(
	  				String.format("LongDescription resource for id '%d' not found", ld.getId()));
		}

		return ldEntRef;
	}

	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	@Override public LongDescriptionBean update(LongDescriptionBean ld) {

		return new LongDescriptionBean().init(updateEntity(ld));
	}

	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	@Override public void delete(long ldId) {

		try {
			LongDescriptionEntity ldEntRef = manager.getReference(LongDescriptionEntity.class, ldId);
	      	manager.remove(ldEntRef);
		}
		catch (EntityNotFoundException ex) {
			throw new ResourceNotFoundException(
	  				String.format("LongDescription resource for id '%d' not found", ldId));
		}
	}

}