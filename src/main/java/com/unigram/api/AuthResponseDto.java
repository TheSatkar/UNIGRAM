package com.unigram.api;

public class AuthResponseDto {
    private SafeUserDto user;
    private String token;

    public AuthResponseDto(SafeUserDto user, String token) {
        this.user = user;
        this.token = token;
    }

    public SafeUserDto getUser() { return user; }
    public String getToken() { return token; }
}
