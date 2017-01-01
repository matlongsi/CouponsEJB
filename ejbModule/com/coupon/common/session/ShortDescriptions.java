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

import com.coupon.common.bean.ShortDescriptionBean;
import com.coupon.common.entity.ShortDescriptionEntity;
import com.coupon.common.exception.ResourceNotFoundException;
import com.coupon.common.session.remote.ShortDescriptionsRemote;


@Remote
@LocalBean
@Stateless(name=ShortDescriptionsRemote.NAME, mappedName=ShortDescriptionsRemote.MAPPED_NAME)
@TransactionManagement(TransactionManagementType.CONTAINER)
public class ShortDescriptions implements ShortDescriptionsRemote {

	@PersistenceContext(unitName="CouponsJPAService")
	private EntityManager manager;

	@EJB private MarketingMaterials mmEJB;
	
	static final Logger logger = Logger.getLogger(ShortDescriptions.class.getName());

	public ShortDescriptions() {
	}
	
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public ShortDescriptionEntity findEntity(long sdId) {

		ShortDescriptionEntity sdEnt = manager.find(ShortDescriptionEntity.class, sdId);		
	  	if (sdEnt == null) {
	  		throw new ResourceNotFoundException(
	  				String.format("ShortDescription resource with id '%d' not found", sdId));
	  	}

		return sdEnt;
	}

	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	@Override public ShortDescriptionBean find(long sdId) {

		return new ShortDescriptionBean().init(findEntity(sdId));
	}

	@TransactionAttribute(TransactionAttributeType.MANDATORY)
	public ShortDescriptionEntity createEntity(ShortDescriptionBean sd) {

		ShortDescriptionEntity sdEnt = new ShortDescriptionEntity();
		sdEnt.setShortDescription(sd.getShortDescription());

		return sdEnt;
	}

	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	@Override public ShortDescriptionBean create(ShortDescriptionBean sd) {

		ShortDescriptionEntity sdEnt = createEntity(sd);
		sdEnt.setMarketingMaterial(mmEJB.findEntity(sd.getParentId()));
		manager.persist(sdEnt);
		manager.flush();
		
		return new ShortDescriptionBean().init(sdEnt);
	}

	@TransactionAttribute(TransactionAttributeType.MANDATORY)
	public ShortDescriptionEntity updateEntity(ShortDescriptionBean sd) {

		ShortDescriptionEntity sdEntRef;
		try {
			sdEntRef = manager.getReference(ShortDescriptionEntity.class, sd.getId());
			sdEntRef.setShortDescription(sd.getShortDescription());
			manager.merge(sdEntRef);
			manager.flush();
		}
		catch (EntityNotFoundException ex) {
			throw new ResourceNotFoundException(
	  				String.format("ShortDescription resource for id '%d' not found", sd.getId()));
		}

		return sdEntRef;
	}

	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	@Override public ShortDescriptionBean update(ShortDescriptionBean sd) {

		return new ShortDescriptionBean().init(updateEntity(sd));
	}

	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	@Override public void delete(long sdId) {

		try {
			ShortDescriptionEntity sdEntRef = manager.getReference(ShortDescriptionEntity.class, sdId);
			manager.remove(sdEntRef);
		}
		catch (EntityNotFoundException ex) {
			throw new ResourceNotFoundException(
	  				String.format("ShortDescription resource for id '%d' not found", sdId));
		}
	}

}