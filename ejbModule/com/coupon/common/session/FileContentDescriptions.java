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

import com.coupon.common.FileContentDescription;
import com.coupon.common.entity.FileContentDescriptionEntity;
import com.coupon.common.bean.FileContentDescriptionBean;
import com.coupon.common.exception.DataValidationException;
import com.coupon.common.exception.ResourceNotFoundException;
import com.coupon.common.session.remote.FileContentDescriptionsRemote;


@Remote
@LocalBean
@Stateless(name=FileContentDescriptionsRemote.NAME, mappedName=FileContentDescriptionsRemote.MAPPED_NAME)
@TransactionManagement(TransactionManagementType.CONTAINER)
public class FileContentDescriptions implements FileContentDescriptionsRemote {

	@PersistenceContext(unitName="CouponsJPAService")
	private EntityManager manager;

	@EJB private Artworks oaEJB;

	static final Logger logger = Logger.getLogger(FileContentDescriptions.class.getName());

	public FileContentDescriptions() {
	}
	
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public FileContentDescriptionEntity findEntity(long fcdId) {

		FileContentDescriptionEntity fcdEnt = manager.find(FileContentDescriptionEntity.class, fcdId);		
	  	if (fcdEnt == null) {
	  		throw new ResourceNotFoundException(
	  				String.format("FileContentDescription resource with id '%d' not found", fcdId));
	  	}

		return fcdEnt;
	}

	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	@Override public FileContentDescriptionBean find(long fcdId) {

		return new FileContentDescriptionBean().init(findEntity(fcdId));
	}

	@TransactionAttribute(TransactionAttributeType.MANDATORY)
	public FileContentDescriptionEntity createEntity(FileContentDescription fcd) {

		FileContentDescriptionEntity fcdEnt = new FileContentDescriptionEntity();
		fcdEnt.setFileContentDescription(fcd.getFileContentDescription());

		return fcdEnt;
	}

	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	@Override public FileContentDescriptionBean create(FileContentDescriptionBean fcd) {

		if (fcd.getParentId() == null) {
			throw new DataValidationException("aId is required.");
		}

		FileContentDescriptionEntity fcdEnt = createEntity(fcd);
		fcdEnt.setArtwork(oaEJB.findEntity(fcd.getParentId()));
		manager.persist(fcdEnt);
		manager.flush();
		
		return new FileContentDescriptionBean().init(fcdEnt);
	}

	@TransactionAttribute(TransactionAttributeType.MANDATORY)
	public FileContentDescriptionEntity updateEntity(FileContentDescriptionBean fcd) {

		FileContentDescriptionEntity fcdEntRef;
		try {

			fcdEntRef = manager.getReference(FileContentDescriptionEntity.class, fcd.getId());
			fcdEntRef.setFileContentDescription(fcd.getFileContentDescription());
			manager.merge(fcdEntRef);
			manager.flush();
		}
		catch (EntityNotFoundException ex) {
			throw new ResourceNotFoundException(
	  				String.format("FileContentDescription resource for id '%d' not found", fcd.getId()));
		}

		return fcdEntRef;
	}

	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	@Override public FileContentDescriptionBean update(FileContentDescriptionBean fcd) {

		if (fcd.getId() == null) {
			throw new DataValidationException("fcdId is required.");
		}

		return new FileContentDescriptionBean().init(updateEntity(fcd));
	}

	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	@Override public void delete(long fcdId) {

		try {
			FileContentDescriptionEntity fcdEntRef = manager.getReference(FileContentDescriptionEntity.class, fcdId);
	      	manager.remove(fcdEntRef);
		}
		catch (EntityNotFoundException ex) {
			throw new ResourceNotFoundException(
	  				String.format("FileContentDescription resource for id '%d' not found", fcdId));
		}
	}

}