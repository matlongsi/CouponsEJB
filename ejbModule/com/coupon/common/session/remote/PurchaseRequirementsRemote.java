package com.coupon.common.session.remote;

import javax.ejb.Remote;

import com.coupon.common.bean.PurchaseRequirementBean;


@Remote
public interface PurchaseRequirementsRemote {

	public static final String NAME = "PurchaseRequirements";
	public static final String MAPPED_NAME = "java:global/CouponsEJB/PurchaseRequirements";
	
	public PurchaseRequirementBean find(long prId);

	public PurchaseRequirementBean create(PurchaseRequirementBean pr);

	public PurchaseRequirementBean update(PurchaseRequirementBean pr);

	public void delete(long prId);

}