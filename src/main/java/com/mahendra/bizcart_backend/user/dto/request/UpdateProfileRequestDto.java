package com.mahendra.bizcart_backend.user.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.mahendra.bizcart_backend.common.constants.AppConstants;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = """
		Fields to change on the authenticated user's profile. Omitted fields are preserved. Sending phone or
		profileImage as null or blank clears that field. Unknown or restricted account fields are rejected.
		""")
public class UpdateProfileRequestDto {

	@Size(max = AppConstants.FieldLengths.NAME, message = "First name must not exceed 100 characters")
	@Pattern(regexp = ".*\\S.*", message = "First name must not be blank")
	@Schema(description = "New first name. Omit to preserve the current value", example = "Mahendra")
	private String firstName;

	@Size(max = AppConstants.FieldLengths.NAME, message = "Last name must not exceed 100 characters")
	@Pattern(regexp = ".*\\S.*", message = "Last name must not be blank")
	@Schema(description = "New last name. Omit to preserve the current value", example = "Singh")
	private String lastName;

	@Size(max = AppConstants.FieldLengths.PHONE, message = "Phone must not exceed 20 characters")
	@Pattern(regexp = "^\\s*$|^\\+?[0-9]{7,15}$", message = "Phone number is invalid")
	@Schema(description = "Unique phone number. Omit to preserve; send null/blank to clear",
			example = "+919876543210", nullable = true)
	private String phone;

	@Size(max = AppConstants.FieldLengths.PROFILE_IMAGE,
			message = "Profile image URL must not exceed 500 characters")
	@Pattern(regexp = "^\\s*$|^https://[^\\s]+$", message = "Profile image must be a valid HTTPS URL")
	@Schema(description = "HTTPS profile image URL. Omit to preserve; send null/blank to clear",
			example = "https://cdn.example.com/profiles/me.png", nullable = true)
	private String profileImage;

	@JsonIgnore
	private boolean firstNamePresent;
	@JsonIgnore
	private boolean lastNamePresent;
	@JsonIgnore
	private boolean phonePresent;
	@JsonIgnore
	private boolean profileImagePresent;

	public UpdateProfileRequestDto() {
	}

	public UpdateProfileRequestDto(String firstName, String lastName, String phone, String profileImage) {
		setFirstName(firstName);
		setLastName(lastName);
		setPhone(phone);
		setProfileImage(profileImage);
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
		this.firstNamePresent = true;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
		this.lastNamePresent = true;
	}

	public String getPhone() {
		return phone;
	}

	public void setPhone(String phone) {
		this.phone = phone;
		this.phonePresent = true;
	}

	public String getProfileImage() {
		return profileImage;
	}

	public void setProfileImage(String profileImage) {
		this.profileImage = profileImage;
		this.profileImagePresent = true;
	}

	public boolean isFirstNamePresent() {
		return firstNamePresent;
	}

	public boolean isLastNamePresent() {
		return lastNamePresent;
	}

	public boolean isPhonePresent() {
		return phonePresent;
	}

	public boolean isProfileImagePresent() {
		return profileImagePresent;
	}

	@JsonIgnore
	@AssertTrue(message = "At least one editable profile field must be supplied")
	public boolean isAnyEditableFieldPresent() {
		return firstNamePresent || lastNamePresent || phonePresent || profileImagePresent;
	}

	@JsonAnySetter
	public void rejectUnknownField(String fieldName, Object value) {
		throw new IllegalArgumentException("Unknown profile field: " + fieldName);
	}
}
