package com.mahendra.bizcart_backend.address.entity;

import com.mahendra.bizcart_backend.address.enums.AddressType;
import com.mahendra.bizcart_backend.common.constants.AppConstants;
import com.mahendra.bizcart_backend.common.entity.BaseEntity;
import com.mahendra.bizcart_backend.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = AppConstants.Tables.ADDRESSES, indexes = {
		@Index(name = AppConstants.Indexes.IX_ADDRESSES_USER_ID, columnList = AppConstants.Columns.USER_ID),
		@Index(name = AppConstants.Indexes.IX_ADDRESSES_USER_DEFAULT,
				columnList = AppConstants.Columns.USER_ID + "," + AppConstants.Columns.DEFAULT_ADDRESS),
		@Index(name = AppConstants.Indexes.IX_ADDRESSES_USER_DELETED,
				columnList = AppConstants.Columns.USER_ID + "," + AppConstants.Columns.DELETED)
})
public class Address extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = AppConstants.Columns.USER_ID, nullable = false,
			foreignKey = @ForeignKey(name = AppConstants.Constraints.FK_ADDRESSES_USER))
	private User user;

	@Column(name = AppConstants.Columns.FULL_NAME, nullable = false, length = AppConstants.FieldLengths.NAME)
	private String fullName;

	@Column(name = AppConstants.Columns.PHONE, nullable = false, length = AppConstants.FieldLengths.PHONE)
	private String phone;

	@Column(name = AppConstants.Columns.ADDRESS_LINE_1, nullable = false,
			length = AppConstants.FieldLengths.ADDRESS_LINE)
	private String addressLine1;

	@Column(name = AppConstants.Columns.ADDRESS_LINE_2, length = AppConstants.FieldLengths.ADDRESS_LINE)
	private String addressLine2;

	@Column(name = AppConstants.Columns.LANDMARK, length = AppConstants.FieldLengths.LANDMARK)
	private String landmark;

	@Column(name = AppConstants.Columns.CITY, nullable = false, length = AppConstants.FieldLengths.CITY)
	private String city;

	@Column(name = AppConstants.Columns.STATE, nullable = false, length = AppConstants.FieldLengths.STATE)
	private String state;

	@Column(name = AppConstants.Columns.POSTAL_CODE, nullable = false, length = AppConstants.FieldLengths.POSTAL_CODE)
	private String postalCode;

	@Column(name = AppConstants.Columns.COUNTRY, nullable = false, length = AppConstants.FieldLengths.COUNTRY)
	private String country;

	@Enumerated(EnumType.STRING)
	@Column(name = AppConstants.Columns.ADDRESS_TYPE, nullable = false, length = AppConstants.FieldLengths.ENUM)
	private AddressType addressType;

	@Column(name = AppConstants.Columns.DEFAULT_ADDRESS, nullable = false)
	private boolean defaultAddress;

	@Column(name = AppConstants.Columns.DELETED, nullable = false)
	private boolean deleted;

	public User getUser() { return user; }
	public void setUser(User user) { this.user = user; }
	public String getFullName() { return fullName; }
	public void setFullName(String fullName) { this.fullName = fullName; }
	public String getPhone() { return phone; }
	public void setPhone(String phone) { this.phone = phone; }
	public String getAddressLine1() { return addressLine1; }
	public void setAddressLine1(String addressLine1) { this.addressLine1 = addressLine1; }
	public String getAddressLine2() { return addressLine2; }
	public void setAddressLine2(String addressLine2) { this.addressLine2 = addressLine2; }
	public String getLandmark() { return landmark; }
	public void setLandmark(String landmark) { this.landmark = landmark; }
	public String getCity() { return city; }
	public void setCity(String city) { this.city = city; }
	public String getState() { return state; }
	public void setState(String state) { this.state = state; }
	public String getPostalCode() { return postalCode; }
	public void setPostalCode(String postalCode) { this.postalCode = postalCode; }
	public String getCountry() { return country; }
	public void setCountry(String country) { this.country = country; }
	public AddressType getAddressType() { return addressType; }
	public void setAddressType(AddressType addressType) { this.addressType = addressType; }
	public boolean isDefaultAddress() { return defaultAddress; }
	public void setDefaultAddress(boolean defaultAddress) { this.defaultAddress = defaultAddress; }
	public boolean isDeleted() { return deleted; }
	public void setDeleted(boolean deleted) { this.deleted = deleted; }
}
