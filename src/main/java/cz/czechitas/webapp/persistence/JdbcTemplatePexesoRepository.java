package cz.czechitas.webapp.persistence;

import java.sql.*;
import java.time.*;
import java.util.*;
import org.mariadb.jdbc.*;
import org.springframework.jdbc.core.*;
import org.springframework.jdbc.support.*;
import org.springframework.stereotype.*;
import cz.czechitas.webapp.entity.*;

@Component
public class JdbcTemplatePexesoRepository implements PexesoRepository {

    private MariaDbDataSource konfiguraceDb;
    JdbcTemplate dotazovac;
    RowMapper<HerniPlocha> mapovacPloch;
    RowMapper<Karta> mapovacKaret;

    public JdbcTemplatePexesoRepository() {
        try {
            konfiguraceDb = new MariaDbDataSource();
            konfiguraceDb.setUserName("student");
            konfiguraceDb.setPassword("password");
            konfiguraceDb.setUrl("jdbc:mysql://localhost:3306/Pexeso");

            dotazovac = new JdbcTemplate(konfiguraceDb);

            mapovacKaret = BeanPropertyRowMapper.newInstance(Karta.class);
            mapovacPloch = BeanPropertyRowMapper.newInstance(HerniPlocha.class);

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    // -----------------------------------------------------------------------------------//

    public HerniPlocha save(HerniPlocha plocha) {
        if (plocha.getId() == null) {
            pridejHerniPlochu(plocha);
        }
        else {
            updatuj(plocha);
        }
        
        return plocha;
    }
    
    
    private HerniPlocha pridejHerniPlochu(HerniPlocha plocha) {
        GeneratedKeyHolder drzakNaVygenerovanyKlic = new GeneratedKeyHolder();
        String sql = "INSERT INTO HerniPlochy (Stav, CasPoslednihoTahu) VALUES (?, ?)";
        dotazovac.update((Connection con) -> {
                    PreparedStatement prikaz = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
                    prikaz.setString(1, plocha.getStav().name());
                    prikaz.setObject(2, Instant.now());
                    return prikaz;
                },
                drzakNaVygenerovanyKlic);
        plocha.setId(drzakNaVygenerovanyKlic.getKey().longValue());

        List<Karta> karticky = plocha.getKarticky();          
        for (int i = 0; i < karticky.size(); i++) {
            Karta karticka = karticky.get(i);
            pridejKarticku(karticka, plocha.getId(), i);
        }
        return plocha;
    }

    private void pridejKarticku(Karta karticka, Long plochaId, int poradiKarty) {
        GeneratedKeyHolder drzakNaVygenerovanyKlic = new GeneratedKeyHolder();
        String sql = "INSERT INTO Karty (CisloKarty, Stav, HerniPlochaID, PoradiKarty) " +
                "VALUES (?, ?, ?, ?)";
        dotazovac.update((Connection con) -> {
                    PreparedStatement prikaz = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
                    prikaz.setInt(1, karticka.getCisloKarty());
                    prikaz.setString(2, karticka.getStav().name());
                    prikaz.setLong(3, plochaId);
                    prikaz.setInt(4, poradiKarty);
                    return prikaz;
                },
                drzakNaVygenerovanyKlic);
        karticka.setId(drzakNaVygenerovanyKlic.getKey().longValue());
    }

    // ------------------------------------------------------------------------------ //

    public HerniPlocha findOne(Long id) {
        HerniPlocha herniPlocha = dotazovac.queryForObject(
                "SELECT ID, Stav FROM HerniPlochy WHERE ID = ?",
                mapovacPloch,
                id);
        List<Karta> karticky = dotazovac.query(
                "SELECT ID, CisloKarty, Stav FROM Karty WHERE HerniPlochaID = ?",
                mapovacKaret,
                id);
        herniPlocha.setKarticky(karticky);
        return herniPlocha;
    }

    // --------------------------------------------------------------------------------- //

    private HerniPlocha updatuj(HerniPlocha plocha) {
        dotazovac.update(
                "UPDATE HerniPlochy SET Stav = ?, CasPoslednihoTahu = ? WHERE ID = ?",
                plocha.getStav().name(),
                Instant.now(),
                plocha.getId());

        List<Karta> karticky = plocha.getKarticky();
        for (int i = 0; i < karticky.size(); i++) {
            Karta karticka = karticky.get(i);
            dotazovac.update(
                    "UPDATE Karty SET Stav = ?, PoradiKarty = ? WHERE ID = ?",
                    karticka.getStav().name(),
                    i,
                    karticka.getId());
        }
        return plocha;
    }
}
