package com.company.workerportal.security;

public final class GenerateHashTool {

    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Usage: GenerateHashTool <plainTextPassword>");
            return;
        }
        System.out.println(PasswordUtil.hash(args[0]));
    }
}
