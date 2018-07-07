package com.fakefingerprintpay.infos;

public class Password {
    private String password;

    public String[] getPasswordPad(String pkg) {
        char[] password = this.password.toCharArray();
        String[] pad = new String[6];
        for (int i = 0; i < 6; i++) {
            switch (pkg) {
                case Appsinfo.alipayPkg:
                    pad[i] = Appsinfo.alipayPad[Integer.parseInt(String.valueOf(password[i]))];
                    break;
                case Appsinfo.taobaoPkg:
                    pad[i] = Appsinfo.taobaoPad[Integer.parseInt(String.valueOf(password[i]))];
                    break;
                case Appsinfo.wechatPkg:
                    pad[i] = Appsinfo.wechatPad[Integer.parseInt(String.valueOf(password[i]))];
                    break;
            }
        }
//        for (String i : pad)
//            System.out.println(i);
        return pad;
    }

    public void setPassword(String password) {
        if (password.length() <= 6)
            this.password = password;
    }
}
