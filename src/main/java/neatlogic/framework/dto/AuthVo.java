package neatlogic.framework.dto;

import neatlogic.framework.auth.core.AuthBase;
import neatlogic.framework.util.$;

public class AuthVo {

    public static final String AUTH_DELETE = "delete";
    public static final String AUTH_ADD = "add";
    public static final String AUTH_COVER = "cover";
    private String name;
    private String displayName;
    private String description;
    private String authGroupName;
    private int userCount;
    private int roleCount;
    private int sort;

    public AuthVo() {

    }

    public AuthVo(String name, String displayName, String description, int sort) {
        this.name = name;
        this.displayName = displayName;
        this.description = description;
        this.sort = sort;
    }

    public AuthVo(String name, String displayName, String description, String authGroupName, int sort) {
        this.name = name;
        this.displayName = displayName;
        this.description = description;
        this.authGroupName = authGroupName;
        this.sort = sort;
    }

    public AuthVo(AuthBase tmpAuth) {
        this.name = tmpAuth.getAuthName();
        this.displayName = tmpAuth.getAuthDisplayName();
        this.description = tmpAuth.getAuthName();
        this.authGroupName = tmpAuth.getAuthGroup();
        this.sort = tmpAuth.getSort();
    }

    public String getDescription() {
        return $.t(description);
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getUserCount() {
        return userCount;
    }

    public void setUserCount(int userCount) {
        this.userCount = userCount;
    }

    public int getRoleCount() {
        return roleCount;
    }

    public void setRoleCount(int roleCount) {
        this.roleCount = roleCount;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDisplayName() {
        return $.t(displayName);
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public int getSort() {
        return sort;
    }

    public void setSort(int sort) {
        this.sort = sort;
    }

    public String getAuthGroupName() {
        return authGroupName;
    }

    public void setAuthGroupName(String authGroupName) {
        this.authGroupName = authGroupName;
    }
}
