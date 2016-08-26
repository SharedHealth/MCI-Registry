package org.sharedhealth.mci.web.model;

public class IdentityStore {
    private String identityToken;

    public void setIdentityToken(String identityToken) {
        this.identityToken = identityToken;
    }

    public String getIdentityToken() {
        return identityToken;
    }

    public String clearIdentityToken() {
        return identityToken = null;
    }

    public boolean hasIdentityToken() {
        return identityToken != null;
    }
}
