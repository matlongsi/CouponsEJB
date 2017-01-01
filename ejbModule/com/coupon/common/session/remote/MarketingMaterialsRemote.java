package com.coupon.common.session.remote;

import javax.ejb.Remote;

import com.coupon.common.bean.MarketingMaterialBean;


@Remote
public interface MarketingMaterialsRemote {

	public static final String NAME = "MarketingMaterials";
	public static final String MAPPED_NAME = "java:global/CouponsEJB/MarketingMaterials";
	
	public MarketingMaterialBean find(long mmId);

	public MarketingMaterialBean create(MarketingMaterialBean mm);

	public MarketingMaterialBean update(MarketingMaterialBean mm);

	public void delete(long mmId);

}