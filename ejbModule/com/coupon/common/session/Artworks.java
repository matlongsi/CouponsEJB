package com.coupon.common.session;

import java.util.HashMap;
import java.util.Map;
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

import com.coupon.common.entity.FileContentDescriptionEntity;
import com.coupon.common.entity.ArtworkEntity;
import com.coupon.common.bean.ArtworkBean;
import com.coupon.common.bean.FileContentDescriptionBean;
import com.coupon.common.exception.DataValidationException;
import com.coupon.common.exception.ResourceNotFoundException;
import com.coupon.common.session.remote.ArtworksRemote;

@Remote
@LocalBean
@Stateless(name=ArtworksRemote.NAME, mappedName=ArtworksRemote.MAPPED_NAME)
@TransactionManagement(TransactionManagementType.CONTAINER)
public class Artworks implements ArtworksRemote {

	@PersistenceContext(unitName="CouponsJPAService")
	private EntityManager manager;
	
	@EJB private FileContentDescriptions fcdEJB;
	@EJB private MarketingMaterials mmEJB;

	static final Logger logger = Logger.getLogger(Artworks.class.getName());

	public Artworks() {
	}
	
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public ArtworkEntity findEntity(long aId) {

		ArtworkEntity aEnt = manager.find(ArtworkEntity.class, aId);		
	  	if (aEnt == null) {
	  		throw new ResourceNotFoundException(
	  				String.format("Artwork resource with id '%d' not found", aId));
	  	}

		return aEnt;
	}

	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	@Override public ArtworkBean find(long aId) {

		return new ArtworkBean().init(findEntity(aId));
	}

	@TransactionAttribute(TransactionAttributeType.MANDATORY)
	public ArtworkEntity createEntity(ArtworkBean a) {

		ArtworkEntity aEnt = new ArtworkEntity();
      	aEnt.setArtworkType(a.getArtworkType());
      	aEnt.setFileName(a.getFileName());
      	aEnt.setFileFormatName(a.getFileFormatName());
      	aEnt.setFileUri(a.getFileUri());

      	long fcdNewKey = 0;
      	Map<Long, FileContentDescriptionEntity> contentDescriptionsMap = new HashMap<Long, FileContentDescriptionEntity>();
      	for (FileContentDescriptionBean fcd : a.getFileContentDescriptions()) {
      		
      		FileContentDescriptionEntity fcdEntity = fcdEJB.createEntity(fcd);
      		fcdEntity.setArtwork(aEnt);
      		contentDescriptionsMap.put(fcdNewKey++, fcdEntity);
      	}
		aEnt.setFileContentDescriptionsMap(contentDescriptionsMap);
      	
		return aEnt;
	}

	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	@Override public ArtworkBean create(ArtworkBean a) {

		if (a.getParentId() == null) {
			throw new DataValidationException("mmId is required.");
		}

		ArtworkEntity aEnt = createEntity(a);
      	aEnt.setMarketingMaterial(mmEJB.findEntity(a.getParentId()));
      	manager.persist(aEnt);
		manager.flush();
		
		return new ArtworkBean().init(aEnt);
	}

	@TransactionAttribute(TransactionAttributeType.MANDATORY)
	public ArtworkEntity updateEntity(ArtworkBean a) {

		ArtworkEntity aEntRef;
		try {
			aEntRef = manager.getReference(ArtworkEntity.class, a.getId());
			aEntRef.setArtworkType(a.getArtworkType());
			aEntRef.setFileName(a.getFileName());
			aEntRef.setFileFormatName(a.getFileFormatName());
			aEntRef.setFileUri(a.getFileUri());
			manager.merge(aEntRef);
			manager.flush();
		}
		catch (EntityNotFoundException ex) {
			throw new ResourceNotFoundException(
	  				String.format("Artwork resource for id '%d' not found", a.getId()));
		}

		return aEntRef;
	}

	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	@Override public ArtworkBean update(ArtworkBean a) {

		if (a.getId() == null) {
			throw new DataValidationException("aId is required.");
		}

		return new ArtworkBean().init(updateEntity(a));
	}

	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	@Override public void delete(long aId) {

		try {
			ArtworkEntity aEntRef = manager.getReference(ArtworkEntity.class, aId);
	      	manager.remove(aEntRef);
		}
		catch (EntityNotFoundException ex) {
			throw new ResourceNotFoundException(
	  				String.format("Artwork resource for id '%d' not found", aId));
		}
	}

}