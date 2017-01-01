package com.coupon.common.session.remote;

import javax.ejb.Remote;

import com.coupon.common.GlobalCouponNumber;
import com.coupon.common.bean.OfferBean;


@Remote
public interface OffersRemote {

	public static final String NAME = "Offers";
	public static final String MAPPED_NAME = "java:global/CouponsEJB/Offers";
	
	public OfferBean find(long oId);

	public OfferBean findByCouponNumber(GlobalCouponNumber gcn);

	public OfferBean create(OfferBean o);

	public OfferBean update(OfferBean o);

	public void delete(long oId);

}