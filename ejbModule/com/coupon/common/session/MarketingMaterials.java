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
import javax.persistence.PersistenceException;

import org.hibernate.exception.ConstraintViolationException;

import com.coupon.common.entity.ArtworkEntity;
import com.coupon.common.entity.LegalStatementEntity;
import com.coupon.common.entity.LongDescriptionEntity;
import com.coupon.common.entity.MarketingMaterialEntity;
import com.coupon.common.entity.OfferEntity;
import com.coupon.common.entity.ProductCategoryEntity;
import com.coupon.common.entity.ShortDescriptionEntity;
import com.coupon.common.bean.ArtworkBean;
import com.coupon.common.bean.LegalStatementBean;
import com.coupon.common.bean.LongDescriptionBean;
import com.coupon.common.bean.MarketingMaterialBean;
import com.coupon.common.bean.ProductCategoryBean;
import com.coupon.common.bean.ShortDescriptionBean;
import com.coupon.common.exception.DataValidationException;
import com.coupon.common.exception.ResourceConflictException;
import com.coupon.common.exception.ResourceNotFoundException;
import com.coupon.common.session.remote.MarketingMaterialsRemote;
import com.coupon.common.utils.ExceptionHelper;

@Remote
@LocalBean
@Stateless(name=MarketingMaterialsRemote.NAME, mappedName=MarketingMaterialsRemote.MAPPED_NAME)
@TransactionManagement(TransactionManagementType.CONTAINER)
public class MarketingMaterials implements MarketingMaterialsRemote {

	@PersistenceContext(unitName="CouponsJPAService")
	private EntityManager manager;

	@EJB private Artworks aEJB;
	@EJB private ShortDescriptions sdEJB;
	@EJB private LongDescriptions ldEJB;
	@EJB private LegalStatements lsEJB;
	@EJB private ProductCategories pcEJB;
	@EJB private Offers oEJB;
	
	static final Logger logger = Logger.getLogger(MarketingMaterials.class.getName());

	public MarketingMaterials() {
	}

	public void validate(MarketingMaterialEntity mme) throws DataValidationException {
	}

	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public MarketingMaterialEntity findEntity(long mmId) {

		MarketingMaterialEntity mmEnt = manager.find(MarketingMaterialEntity.class, mmId);		
	  	if (mmEnt == null) {
	  		throw new ResourceNotFoundException(
	  				String.format("MarketingMaterial resource with id '%d' not found", mmId));
	  	}

		return mmEnt;
	}

	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	@Override public MarketingMaterialBean find(long mmId) {

		return new MarketingMaterialBean().init(findEntity(mmId));
	}

	@TransactionAttribute(TransactionAttributeType.MANDATORY)
	public MarketingMaterialEntity createEntity(MarketingMaterialBean mm) {

		MarketingMaterialEntity mmEnt = new MarketingMaterialEntity();

      	long sdNewKey = 0;
      	Map<Long, ShortDescriptionEntity> shortDescriptionsMap = new HashMap<Long, ShortDescriptionEntity>();
      	for (ShortDescriptionBean sd : mm.getShortDescriptions()) {

      		ShortDescriptionEntity sdEntity = sdEJB.createEntity(sd);
      		sdEntity.setMarketingMaterial(mmEnt);
      		shortDescriptionsMap.put(sdNewKey++, sdEntity);
      	}
  		mmEnt.setShortDescriptionsMap(shortDescriptionsMap);

      	long ldNewKey = 0;
      	Map<Long, LongDescriptionEntity> longDescriptionsMap = new HashMap<Long, LongDescriptionEntity>();
      	for (LongDescriptionBean ld : mm.getLongDescriptions()) {

      		LongDescriptionEntity ldEntity = ldEJB.createEntity(ld);
      		ldEntity.setMarketingMaterial(mmEnt);
      		longDescriptionsMap.put(ldNewKey++, ldEntity);
      	}
  		mmEnt.setLongDescriptionsMap(longDescriptionsMap);

      	long lsNewKey = 0;
      	Map<Long, LegalStatementEntity> legalStatementsMap = new HashMap<Long, LegalStatementEntity>();
      	for (LegalStatementBean ls : mm.getLegalStatements()) {

      		LegalStatementEntity lsEntity = lsEJB.createEntity(ls);
      		lsEntity.setMarketingMaterial(mmEnt);
      		legalStatementsMap.put(lsNewKey++, lsEntity);
      	}
  		mmEnt.setLegalStatementsMap(legalStatementsMap);

      	long pcNewKey = 0;
      	Map<Long, ProductCategoryEntity> productCategoriesMap = new HashMap<Long, ProductCategoryEntity>();
      	for (ProductCategoryBean pc : mm.getProductCategories()) {

      		ProductCategoryEntity pcEntity = pcEJB.createEntity(pc);
      		pcEntity.setMarketingMaterial(mmEnt);
      		productCategoriesMap.put(pcNewKey++, pcEntity);
      	}
  		mmEnt.setProductCategoriesMap(productCategoriesMap);

      	long aNewKey = 0;
      	Map<Long, ArtworkEntity> artworksMap = new HashMap<Long, ArtworkEntity>();
      	for (ArtworkBean a : mm.getArtworks()) {

      		ArtworkEntity aEntity = aEJB.createEntity(a);
      		aEntity.setMarketingMaterial(mmEnt);
      		artworksMap.put(aNewKey++, aEntity);
      	}
  		mmEnt.setArtworksMap(artworksMap);
  		mmEnt.setBrandName(mm.getBrandName());

		return mmEnt;
	}

	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	@Override public MarketingMaterialBean create(MarketingMaterialBean mm) {

		MarketingMaterialEntity mmEnt = createEntity(mm);
		OfferEntity oEnt = oEJB.findEntity(mm.getParentId());
		mmEnt.setOffer(oEnt);
		validate(mmEnt);

      	try {
      		manager.persist(mmEnt);
      		manager.flush();
      	}
      	catch (PersistenceException ex) {
      		ConstraintViolationException cvx = ExceptionHelper
      				.unrollException(ex, ConstraintViolationException.class);
      		if ((cvx != null) && cvx.getConstraintName().equals("SINGLE_MARKETING_MATERIAL")) {
	    			throw new ResourceConflictException(
	    	  				String.format("MarketingMaterial for Offer number '%s' already exists",
	    	  						oEnt.getOfferNumber().toString()));
      		}
      		throw ex;
      	}
		
		return new MarketingMaterialBean().init(mmEnt);
	}

	@TransactionAttribute(TransactionAttributeType.MANDATORY)
	public MarketingMaterialEntity updateEntity(MarketingMaterialBean mm) {

		MarketingMaterialEntity mmEntRef = null;
		try {
			mmEntRef = manager.getReference(MarketingMaterialEntity.class, mm.getId());
	      	for (ShortDescriptionBean sd : mm.getShortDescriptions()) {
	      		ShortDescriptionEntity sdEntityRef = sdEJB.updateEntity(sd);
	      		mmEntRef.getShortDescriptionsMap().put(sdEntityRef.getId(), sdEntityRef);
	      	}
	      	for (LongDescriptionBean ld : mm.getLongDescriptions()) {
	      		LongDescriptionEntity ldEntityRef = ldEJB.updateEntity(ld);
	      		mmEntRef.getLongDescriptionsMap().put(ldEntityRef.getId(), ldEntityRef);
	      	}
	      	for (LegalStatementBean ls : mm.getLegalStatements()) {
	      		LegalStatementEntity lsEntityRef = lsEJB.updateEntity(ls);
	      		mmEntRef.getLegalStatementsMap().put(lsEntityRef.getId(), lsEntityRef);
	      	}
	      	for (ProductCategoryBean pc : mm.getProductCategories()) {
	      		ProductCategoryEntity pcEntityRef = pcEJB.updateEntity(pc);
	      		mmEntRef.getProductCategoriesMap().put(pcEntityRef.getId(), pcEntityRef);
	      	}
	      	for (ArtworkBean a : mm.getArtworks()) {
	      		ArtworkEntity aEntityRef = aEJB.updateEntity(a);
	      		mmEntRef.getArtworksMap().put(aEntityRef.getId(), aEntityRef);
	      	}
	  		mmEntRef.setBrandName(mm.getBrandName());
	  		manager.merge(mmEntRef);
	  		manager.flush();
		}
		catch (EntityNotFoundException ex) {
			throw new ResourceNotFoundException(
	  				String.format("MarketingMaterial resource for id '%d' not found", mm.getId()));
		}
      	catch (PersistenceException ex) {

      		ConstraintViolationException cvx = ExceptionHelper
      				.unrollException(ex, ConstraintViolationException.class);
      		if ((cvx != null) && cvx.getConstraintName().equals("SINGLE_MARKETING_MATERIAL")) {
	    			throw new ResourceConflictException(
	    	  				String.format("MarketingMaterial for Offer number '%s' already exists",
	    	  						mmEntRef.getOffer().getOfferNumber().toString()));
      		}
      		throw ex;
      	}

		return mmEntRef;
	}

	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	@Override public MarketingMaterialBean update(MarketingMaterialBean mm) {

		return new MarketingMaterialBean().init(updateEntity(mm));
	}

	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	@Override public void delete(long mmId) {

      	try {
      		MarketingMaterialEntity mmEntRef = manager.getReference(MarketingMaterialEntity.class, mmId);
          	manager.remove(mmEntRef);
      	}
		catch (EntityNotFoundException ex) {
			throw new ResourceNotFoundException(
	  				String.format("MarketingMaterial resource for id '%d' not found", mmId));
		}
	}

}