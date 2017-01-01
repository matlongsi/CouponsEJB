package com.coupon.common.session.remote;

import javax.ejb.Remote;

import com.coupon.common.bean.ArtworkBean;


@Remote
public interface ArtworksRemote {

	public static final String NAME = "Artworks";
	public static final String MAPPED_NAME = "java:global/CouponsEJB/Artworks";
	
	public ArtworkBean find(long aId);

	public ArtworkBean create(ArtworkBean a);

	public ArtworkBean update(ArtworkBean a);

	public void delete(long aId);

}