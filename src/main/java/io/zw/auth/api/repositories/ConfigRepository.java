package io.zw.auth.api.repositories;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.zw.auth.api.models.UserModel;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class ConfigRepository implements PanacheRepository<UserModel> {

    public List<UserModel> getAll() {
        return listAll();
    }

    public UserModel findByUserId(String userId) {
        return find("userId", userId).firstResult();
    }

    public UserModel findByUsername(String username) {
        return find("username", username).firstResult();
    }

    public UserModel findBySocialIdAndSocialType(String socialId, int socialType) {
        return find("socialId = ?1 and socialType= ?2", socialId, socialType).firstResult();
    }

    public void add(UserModel item) {
        persist(item);
    }
}
