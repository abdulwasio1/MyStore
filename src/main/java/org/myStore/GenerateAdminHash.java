package org.myStore;

import org.myStore.auth.PasswordUtil;

public class GenerateAdminHash {

    public static void main(String[] args) {

        String hash = PasswordUtil.hashPassword("admin123");

        System.out.println(hash);
    }
}