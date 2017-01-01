package com.coupon.common.session.remote;

import javax.ejb.Remote;

import com.coupon.common.bean.ProductCategoryBean;


@Remote
public interface ProductCategoriesRemote {

	public static final String NAME = "ProductCategories";
	public static final String MAPPED_NAME = "java:global/CouponsEJB/ProductCategories";
	
	public ProductCategoryBean find(long pcId);

	public ProductCategoryBean create(ProductCategoryBean pc);

	public ProductCategoryBean update(ProductCategoryBean pc);

	public void delete(long pcId);

}