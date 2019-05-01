package cz.czechitas.webapp.logika;

import java.util.*;
import org.springframework.stereotype.*;
import cz.czechitas.webapp.entity.*;
import cz.czechitas.webapp.persistence.*;

@Component
public class PexesoService {

    private PexesoRepository ulozisteHer;                

    public PexesoService(JdbcTemplatePexesoRepository ulozisteHer) {
        this.ulozisteHer = ulozisteHer;
    }

    public HerniPlocha vytvorNovouHerniPlochu() {                                                // metoda, která vrací herní plochu
        List<Karta> karticky = new ArrayList<>();                                               // herní plocha má id, stav a list
        int cisloKarty = 0;                                                                      // list je tvořen kartičkami
        for (int i = 0; i < 8; i++) {
            karticky.add(vytvorKartu(cisloKarty));                                                // vkládá kartu, která je vytvořena zdejší metodou
            cisloKarty++;                                                                           // metoda bere číslo karty (aby každá karta v listu měla své
            karticky.add(vytvorKartu(cisloKarty));                                                // karta má své id, číslo karty v listu a stav (rub / líc)
            cisloKarty++;                                                                       // v cyklu se vždy přidají 2 karty
        }
        Collections.shuffle(karticky);
        HerniPlocha novaPlocha = new HerniPlocha(karticky, StavHry.HRAC1_VYBER_PRVNI_KARTY);    // zde vytvořená plocha, do které je vložen list (kartičky) a stav hry
        novaPlocha = ulozisteHer.save(novaPlocha);                                                   // zde je herní plocha uložena do repository
        return novaPlocha;                                                                         // vrácení dané plochy, se kterou se pak pracuje
    }

    private Karta vytvorKartu(int cisloKarty) {
        return new Karta(cisloKarty, StavKarty.RUBEM_NAHORU);
    }

    public HerniPlocha najdiHerniPlochu(Long id) {
        HerniPlocha aktualniPlocha = ulozisteHer.findOne(id);                                   // pokud budeme chtít určitou plochu, se kterou jsme již hráli
        return aktualniPlocha;
    }

    public void provedTah(Long idHerniPlochy, int poziceKartyNaKterouSeKliknulo) {           // na tah potřebujeme znát na jaké ploše hrajeme a na jakou pozici karty jsme klikli
        HerniPlocha aktualniPlocha = ulozisteHer.findOne(idHerniPlochy);                     //najdeme plochu dle id

        if (aktualniPlocha.getStav() == StavHry.HRAC1_VYBER_PRVNI_KARTY) {                  //pokud hráč vybírá první kartu
            vyberPrvniKartu(poziceKartyNaKterouSeKliknulo, aktualniPlocha);                   // zavolá se metoda, která vybírá 1. kartu
        } else if (aktualniPlocha.getStav() == StavHry.HRAC1_VYBER_DRUHE_KARTY) {
            vyberDruhouKartu(poziceKartyNaKterouSeKliknulo, aktualniPlocha);
        } else if (aktualniPlocha.getStav() == StavHry.HRAC1_ZOBRAZENI_VYHODNOCENI) {       //pokud jsou obrácené 2 karty
            List<Karta> karticky = vyhodnotOtoceneKarticky(aktualniPlocha);                  // vyhodnotí se stav - zavolá s emetoda
                                                                                       
            if (!jeKonecHry(karticky)) {                                                    // pokud jeKOnecHry = false --> hraje se dál
                aktualniPlocha.setStav(StavHry.HRAC1_VYBER_PRVNI_KARTY);                     // pokud na ploše zústaly nějaké karty - stav hry je zas na výběr
            } else {
                aktualniPlocha.setStav(StavHry.KONEC);                                       // pokud už žádné karty nejsou - stav hry je konec
            }
        }
                                                                                                  // neukládám kartičky do aktuální plochy ? - ne nepotřebuji, protože počet karet je fůrt stejný, mění se jen stav karet - nakonec řeším jen to, jeslti všechny karty mají stav "odebráno"
        ulozisteHer.save(aktualniPlocha);                                                          // uloží se aktuální plocha - ukládá kolik karet
    }

    private void vyberPrvniKartu(int cisloKartyNaKterouSeKliknulo, HerniPlocha aktualniPlocha) {
        Karta karticka = aktualniPlocha.najdi(cisloKartyNaKterouSeKliknulo);                         // hledá se karta v ploše- tedy v listu s daným číslem, které bylo kartě v listu přiděleno
        if (karticka.getStav() == StavKarty.RUBEM_NAHORU) {
            karticka.setStav(StavKarty.LICEM_NAHORU);
            aktualniPlocha.setStav(StavHry.HRAC1_VYBER_DRUHE_KARTY);
        }
    }

    private void vyberDruhouKartu(int cisloKartyNaKterouSeKliknulo, HerniPlocha aktualniPlocha) {
        Karta karticka = aktualniPlocha.najdi(cisloKartyNaKterouSeKliknulo);
        if (karticka.getStav() == StavKarty.RUBEM_NAHORU) {
            karticka.setStav(StavKarty.LICEM_NAHORU);
            aktualniPlocha.setStav(StavHry.HRAC1_ZOBRAZENI_VYHODNOCENI);

        }
    }

    private List<Karta> vyhodnotOtoceneKarticky(HerniPlocha aktualniPlocha) {
        List<Karta> karticky = aktualniPlocha.getKarticky();                                          // do nového listu se uloží kartičky, které jsou v aktuální ploše
        Karta karta1 = karticky.get(0);                                                                 // z listu se vybere karta, která je na 1. pozici
        Karta karta2 = karticky.get(1);                                                                 // která je na 2. pozici

        int i = 0;
        for (; i < karticky.size(); i++) {                                                              // cyklus, která projíždí kartičky, a zjišťuje, jestli jednotlivé karty jsou lícem nahoru, pokud ano, tak se zastaví cyklus
            karta1 = karticky.get(i);
            if (karta1.getStav() == StavKarty.LICEM_NAHORU) break;
        }
        int j = i + 1;
        for (; j < karticky.size(); j++) {                                                               // nový cyklus, který projíždí opět kartičky, ale od poslední otočené, pokud narazí na lícem nahoru, zastaví se
            karta2 = karticky.get(j);
            if (karta2.getStav() == StavKarty.LICEM_NAHORU) break;
        }
        if (karta1.getCisloObrazku() == karta2.getCisloObrazku()) {                                      // karta obsahuje metodu, která dává číslo obrázku - tj číslo karty, které je dělené dvěma
            karta1.setStav(StavKarty.ODEBRANA);                                                         // pokud se čísla rovnají, oboum se nastaví odebráno
            karta2.setStav(StavKarty.ODEBRANA);
        } else {
            karta1.setStav(StavKarty.RUBEM_NAHORU);                                                        // pokud ne, otočí se zpět
            karta2.setStav(StavKarty.RUBEM_NAHORU);
        }
        return karticky;                                                                                     // vrátí se list s kartičkama
    }

    private boolean jeKonecHry(List<Karta> karticky) {
        boolean jeKonec = true;
        for (Karta karta : karticky) {                                                                      // projíždím všechny karty, jestli mají stav odebráno
            if (karta.getStav() != StavKarty.ODEBRANA) {                                                     // pokud karta nemá stav odebráno, tak  není konec hry - tedy jeKOnec=false
                jeKonec = false;
            }
        }
        return jeKonec;
    }
}

