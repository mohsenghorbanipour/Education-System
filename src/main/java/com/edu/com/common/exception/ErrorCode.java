package com.edu.com.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "The request is invalid"),
    VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "Please check the submitted fields"),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "Invalid university number or password"),
    AUTHENTICATION_REQUIRED(HttpStatus.UNAUTHORIZED, "Please login first"),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "The authentication token is invalid"),
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "Your session has expired. Please login again"),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "You do not have permission to perform this action"),
    ACCOUNT_DISABLED(HttpStatus.FORBIDDEN, "User account is disabled"),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "The requested resource was not found"),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "This HTTP method is not supported"),
    NOT_ACCEPTABLE(HttpStatus.NOT_ACCEPTABLE, "The requested response format is not supported"),
    CONFLICT(HttpStatus.CONFLICT, "The request conflicts with existing data"),
    UNIVERSITY_NUMBER_ALREADY_EXISTS(HttpStatus.CONFLICT, "This university number is already registered"),
    NATIONAL_CODE_ALREADY_EXISTS(HttpStatus.CONFLICT, "This national code is already registered"),
    MOBILE_NUMBER_ALREADY_EXISTS(HttpStatus.CONFLICT, "This mobile number is already registered"),
    MAJOR_NOT_FOUND(HttpStatus.NOT_FOUND, "The selected major was not found"),
    MAJOR_NAME_ALREADY_EXISTS(HttpStatus.CONFLICT, "This major name is already registered"),
    MAJOR_IN_USE(HttpStatus.CONFLICT, "This major cannot be deleted because it is assigned to users or courses"),
    COURSE_CODE_ALREADY_EXISTS(HttpStatus.CONFLICT, "This course code is already registered"),
    COURSE_NOT_FOUND(HttpStatus.NOT_FOUND, "Course was not found"),
    COURSE_IN_USE(HttpStatus.CONFLICT, "This course cannot be deleted because it is in use"),
    COURSE_OFFERING_NOT_FOUND(HttpStatus.NOT_FOUND, "Course offering was not found"),
    COURSE_OFFERING_ALREADY_EXISTS(HttpStatus.CONFLICT, "This course is already offered in the selected semester"),
    COURSE_OFFERING_IN_USE(HttpStatus.CONFLICT, "This course offering is selected by students and cannot be deleted or assigned to a different course or semester"),
    SEMESTER_TITLE_ALREADY_EXISTS(HttpStatus.CONFLICT, "This semester title is already registered"),
    SEMESTER_NOT_FOUND(HttpStatus.NOT_FOUND, "Semester was not found"),
    SEMESTER_IN_USE(HttpStatus.CONFLICT, "This semester cannot be deleted because it has course offerings or enrollment records"),
    INVALID_SEMESTER_DATES(HttpStatus.BAD_REQUEST, "Semester end date must be after start date"),
    ENROLLMENT_ALREADY_EXISTS(HttpStatus.CONFLICT, "This student already has an enrollment in the selected semester"),
    ENROLLMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "Enrollment was not found"),
    ENROLLMENT_ITEM_NOT_FOUND(HttpStatus.NOT_FOUND, "Selected course was not found in this enrollment"),
    ENROLLMENT_IN_USE(HttpStatus.CONFLICT, "Remove the selected courses before deleting this enrollment"),
    ENROLLMENT_CHANGE_CONFLICT(HttpStatus.CONFLICT, "The change would invalidate an existing enrollment"),
    COURSE_OFFERING_CAPACITY_FULL(HttpStatus.CONFLICT, "This course offering has reached its capacity"),
    COURSE_OFFERING_CAPACITY_BELOW_ENROLLED_COUNT(HttpStatus.CONFLICT, "Capacity cannot be less than the number of enrolled students"),
    ENROLLMENT_CREDIT_LIMIT_EXCEEDED(HttpStatus.CONFLICT, "Selected credit units exceed the enrollment plan limit"),
    ENROLLMENT_COURSE_MAJOR_MISMATCH(HttpStatus.CONFLICT, "This enrollment plan only permits courses in the student's major"),
    ENROLLMENT_OUTSIDE_MAJOR_LIMIT_EXCEEDED(HttpStatus.CONFLICT, "The honors plan permits at most one course outside the student's major"),
    ENROLLMENT_GUEST_COURSE_NOT_ALLOWED(HttpStatus.CONFLICT, "The guest plan only permits courses marked as available to guests"),
    ENROLLMENT_COURSE_SEMESTER_MISMATCH(HttpStatus.CONFLICT, "All selected courses must be offered in the enrollment semester"),
    ENROLLMENT_DUPLICATE_COURSE(HttpStatus.CONFLICT, "A course cannot be selected more than once in an enrollment"),
    ENROLLMENT_STUDENT_REQUIRED(HttpStatus.BAD_REQUEST, "Enrollment can only be created for a student"),
    STUDENT_MAJOR_REQUIRED(HttpStatus.CONFLICT, "The student must have a major before enrollment"),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "User was not found"),
    USER_IN_USE(HttpStatus.CONFLICT, "This user cannot be deleted because they have enrollment records"),
    UNSUPPORTED_MEDIA_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "The request content type is not supported"),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "An internal server error occurred"),
    HTTP_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "The request could not be completed");

    private final HttpStatus status;
    private final String message;

    public static ErrorCode forStatus(int status) {
        return switch (status) {
            case 400 -> INVALID_REQUEST;
            case 401 -> AUTHENTICATION_REQUIRED;
            case 403 -> ACCESS_DENIED;
            case 404 -> RESOURCE_NOT_FOUND;
            case 405 -> METHOD_NOT_ALLOWED;
            case 406 -> NOT_ACCEPTABLE;
            case 409 -> CONFLICT;
            case 415 -> UNSUPPORTED_MEDIA_TYPE;
            default -> status >= 500 ? INTERNAL_ERROR : HTTP_ERROR;
        };
    }
}
