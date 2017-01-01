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

import com.coupon.common.entity.ProductCategoryEntity;
import com.coupon.common.bean.ProductCategoryBean;
import com.coupon.common.exception.ResourceNotFoundException;
import com.coupon.common.session.remote.ProductCategoriesRemote;


@Remote
@LocalBean
@Stateless(name=ProductCategoriesRemote.NAME, mappedName=ProductCategoriesRemote.MAPPED_NAME)
@TransactionManagement(TransactionManagementType.CONTAINER)
public class ProductCategories implements ProductCategoriesRemote {

	@PersistenceContext(unitName="CouponsJPAService")
	private EntityManager manager;

	@EJB private MarketingMaterials mmEJB;
	
	static final Logger logger = Logger.getLogger(ProductCategories.class.getName());

	public ProductCategories() {
	}
	
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public ProductCategoryEntity findEntity(long pcId) {

		ProductCategoryEntity pcEnt = manager.find(ProductCategoryEntity.class, pcId);		
	  	if (pcEnt == null) {
	  		throw new ResourceNotFoundException(
	  				String.format("ProductCategory resource with id '%d' not found", pcId));
	  	}

		return pcEnt;
	}

	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	@Override public ProductCategoryBean find(long pcId) {

		return new ProductCategoryBean().init(findEntity(pcId));
	}

	@TransactionAttribute(TransactionAttributeType.MANDATORY)
	public ProductCategoryEntity createEntity(ProductCategoryBean pc) {

		ProductCategoryEntity pcEnt = new ProductCategoryEntity();
		pcEnt.setCategoryName(pc.getCategoryName());

		return pcEnt;
	}

	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	@Override public ProductCategoryBean create(ProductCategoryBean pc) {

		ProductCategoryEntity pcEnt = createEntity(pc);
		pcEnt.setMarketingMaterial(mmEJB.findEntity(pc.getParentId()));
		manager.persist(pcEnt);
		manager.flush();
		
		return new ProductCategoryBean().init(pcEnt);
	}

	@TransactionAttribute(TransactionAttributeType.MANDATORY)
	public ProductCategoryEntity updateEntity(ProductCategoryBean pc) {

		ProductCategoryEntity pcEntRef;
		try {

			pcEntRef = manager.getReference(ProductCategoryEntity.class, pc.getId());
			pcEntRef.setCategoryName(pc.getCategoryName());
			manager.merge(pcEntRef);
			manager.flush();
		}
		catch (EntityNotFoundException ex) {
			throw new ResourceNotFoundException(
	  				String.format("ProductCategory resource for id '%d' not found", pc.getId()));
		}

		return pcEntRef;
	}

	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	@Override public ProductCategoryBean update(ProductCategoryBean pc) {

		return new ProductCategoryBean().init(updateEntity(pc));
	}

	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	@Override public void delete(long pcId) {
      	
		try {
			ProductCategoryEntity pcEntRef = manager.getReference(ProductCategoryEntity.class, pcId);
	      	manager.remove(pcEntRef);
		}
		catch (EntityNotFoundException ex) {
			throw new ResourceNotFoundException(
	  				String.format("ProductCategory resource for id '%d' not found", pcId));
		}
	}

}