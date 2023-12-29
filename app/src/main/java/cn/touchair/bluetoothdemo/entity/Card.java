package cn.touchair.bluetoothdemo.entity;

import androidx.annotation.NonNull;

public class Card {
    private String company;
    private String name;
    private String position;
    private String mailingAddress;
    private String telephone;
    private String email;

    public Card(String company, String name, String position, String mailingAddress, String telephone, String email) {
        this.company = company;
        this.name = name;
        this.position = position;
        this.mailingAddress = mailingAddress;
        this.telephone = telephone;
        this.email = email;
    }

    public String getCompany() {
        return company;
    }

    public void setCompany(CharSequence company) {
        this.company = company.toString();
    }

    public String getName() {
        return name;
    }

    public void setName(CharSequence name) {
        this.name = name.toString();
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(CharSequence position) {
        this.position = position.toString();
    }

    public String getMailingAddress() {
        return mailingAddress;
    }

    public void setMailingAddress(CharSequence mailingAddress) {
        this.mailingAddress = mailingAddress.toString();
    }

    public String getTelephone() {
        return telephone;
    }

    public void setTelephone(CharSequence telephone) {
        this.telephone = telephone.toString();
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(CharSequence email) {
        this.email = email.toString();
    }

    public static class Builder {
        private final String company;
        private final String name;
        private String position = "Not Specified";
        private String mailingAddress = "Not Specified";
        private String telephone = "Not Specified";
        private String email = "Not Specified";

        public Builder(@NonNull String company, @NonNull String name) {
            this.company = company;
            this.name = name;
        }

        public Builder setPosition(String position) {
            this.position = position;
            return this;
        }

        public Builder setMailingAddress(String mailingAddress) {
            this.mailingAddress = mailingAddress;
            return this;
        }

        public Builder setTelephone(String telephone) {
            this.telephone = telephone;
            return this;
        }

        public Builder setEmail(String email) {
            this.email = email;
            return this;
        }

        public Card build() {
            return new Card(company, name, position, mailingAddress, telephone, email);
        }
    }
}
