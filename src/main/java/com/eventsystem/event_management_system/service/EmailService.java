package com.eventsystem.event_management_system.service;

import com.eventsystem.event_management_system.dto.EmailDetalj;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String from;

    /**
     * Podaci potrebni za email potvrde registracije. Prosleđuju se kao
     * vrednosni objekat (ne entitet) jer se slanje izvršava asinhrono, van
     * Hibernate sesije, pa lazy asocijacije ne bi bile dostupne.
     */
    public record PotvrdaRegistracije(
            String emailPrimaoca,
            String ime,
            String prezime,
            String brojKarte,
            String nazivDogadjaja,
            String datumPocetka,
            String datumZavrsetka,
            String lokacija,
            String nazivTipa,
            String vrstaKarte,
            String cena
    ) {}

    /**
     * Generičko email obaveštenje (naslov + tekst). Koristi se za notifikacije
     * koje treba isporučiti i mejlom (npr. D3 — oslobođeno mesto sa liste čekanja).
     */
    public record ObavestenjeEmail(
            String emailPrimaoca,
            String ime,
            String naslov,
            String poruka,
            String tipLabel,            // kategorija obaveštenja (npr. "Događaj")
            List<EmailDetalj> detalji   // strukturirani redovi za karticu (može biti prazno)
    ) {}

    /**
     * Šalje učesniku potvrdu o uspešnoj registraciji na događaj zajedno sa
     * podacima o kupljenoj karti. Greška pri slanju se loguje, ali ne prekida
     * poslovnu transakciju (poziva se asinhrono).
     */
    @Async
    public void posaljiPotvrduRegistracije(PotvrdaRegistracije p) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
            helper.setFrom(from);
            helper.setTo(p.emailPrimaoca());
            helper.setSubject("Potvrda registracije - " + p.nazivDogadjaja());
            helper.setText(buildHtml(p), true);

            mailSender.send(message);
            log.info("Poslat email potvrde registracije (karta {}) na {}",
                    p.brojKarte(), p.emailPrimaoca());
        } catch (Exception e) {
            log.error("Neuspešno slanje emaila za kartu {}: {}",
                    p.brojKarte(), e.getMessage(), e);
        }
    }

    /**
     * Šalje generičko obaveštenje na email. Greška se loguje, ali ne prekida
     * poslovni tok (poziva se asinhrono, posle commit-a transakcije).
     */
    @Async
    public void posaljiObavestenje(ObavestenjeEmail o) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
            helper.setFrom(from);
            helper.setTo(o.emailPrimaoca());
            helper.setSubject(o.naslov());
            helper.setText(buildObavestenjeHtml(o), true);

            mailSender.send(message);
            log.info("Poslat email obaveštenja '{}' na {}", o.naslov(), o.emailPrimaoca());
        } catch (Exception e) {
            log.error("Neuspešno slanje email obaveštenja na {}: {}",
                    o.emailPrimaoca(), e.getMessage(), e);
        }
    }

    private String buildObavestenjeHtml(ObavestenjeEmail o) {
        String tipLabel = (o.tipLabel() != null && !o.tipLabel().isBlank())
                ? o.tipLabel() : "Obaveštenje";

        String redovi = "";
        if (o.detalji() != null) {
            for (EmailDetalj d : o.detalji()) {
                if (d != null && d.vrednost() != null && !d.vrednost().isBlank()) {
                    redovi += detaljRed(d.oznaka(), d.vrednost());
                }
            }
        }

        // Kartica sa detaljima se prikazuje samo ako postoji bar jedan red.
        String infoKartica = redovi.isEmpty() ? "" : """
            <div style="
                margin-top:30px;border:2px dashed #d6d6d6;border-radius:14px;
                padding:20px 25px;background:#fafbff;
            ">
                <table style="width:100%%;border-collapse:collapse;font-size:15px;">
                    %s
                </table>
            </div>
            """.formatted(redovi);

        return """
        <div style="font-family:'Segoe UI',Arial,sans-serif;padding:30px 15px;">
            <div style="
                max-width:650px;margin:auto;background:#ffffff;border-radius:16px;
                overflow:hidden;box-shadow:0 10px 30px rgba(0,0,0,0.12);
            ">
                <div style="
                    background:linear-gradient(135deg,#0b1020 0%%,#131b32 60%%,#312e81 100%%);
                    padding:35px;text-align:center;color:white;
                ">
                    <div style="
                        display:inline-block;padding:5px 14px;margin-bottom:14px;
                        border-radius:999px;background:rgba(129,140,248,0.25);
                        color:#c7d2fe;font-size:12px;font-weight:600;
                        text-transform:uppercase;letter-spacing:1px;
                    ">%s</div>
                    <h1 style="margin:0;font-size:26px;font-weight:700;">%s</h1>
                </div>
                <div style="padding:35px;">
                    <p style="font-size:16px;">Poštovani/a <strong>%s</strong>,</p>
                    <p style="color:#555;line-height:1.7;font-size:15px;">%s</p>
                    %s
                </div>
                <div style="
                    text-align:center;padding:20px;background:#f8fafc;
                    color:#94a3b8;font-size:12px;
                ">
                    Hvala što koristite EventSys.<br>
                    Ovo je automatski generisana poruka, molimo Vas da ne odgovarate na nju.
                </div>
            </div>
        </div>
        """.formatted(tipLabel, o.naslov(), o.ime(), o.poruka(), infoKartica);
    }

    private String detaljRed(String oznaka, String vrednost) {
        return """
        <tr>
            <td style="padding:9px 0;color:#666;">%s</td>
            <td style="text-align:right;font-weight:600;">%s</td>
        </tr>
        """.formatted(oznaka, vrednost);
    }

    private String buildHtml(PotvrdaRegistracije p) {

        return """
        <div style="
            font-family: 'Segoe UI', Arial, sans-serif;
            padding: 30px 15px;
        ">

            <div style="
                max-width: 650px;
                margin: auto;
                background: #ffffff;
                border-radius: 16px;
                overflow: hidden;
                box-shadow: 0 10px 30px rgba(0,0,0,0.12);
            ">

                <!-- Header -->
                <div style="
                    background: linear-gradient(
                        135deg,
                        #0b1020 0%%,
                        #131b32 60%%,
                        #312e81 100%%
                    );
                    padding: 35px;
                    text-align: center;
                    color: white;
                ">
                    <h1 style="
                        margin: 0;
                        font-size: 28px;
                        font-weight: 700;
                    ">
                        Registracija uspešna
                    </h1>

                    <p style="
                        margin-top: 10px;
                        opacity: 0.9;
                        font-size: 15px;
                    ">
                        Vaša karta je uspešno kreirana
                    </p>
                </div>

                <!-- Body -->
                <div style="padding: 35px;">

                    <p style="font-size:16px;">
                        Poštovani/a <strong>%s %s</strong>,
                    </p>

                    <p style="
                        color:#555;
                        line-height:1.7;
                        font-size:15px;
                    ">
                        Uspešno ste registrovani za događaj
                        <strong>%s</strong>.
                        U nastavku se nalaze informacije o Vašoj karti.
                    </p>

                    <!-- Ticket -->
                    <div style="
                        margin-top:30px;
                        border:2px dashed #d6d6d6;
                        border-radius:14px;
                        padding:25px;
                        background:#fafbff;
                    ">

                        <div style="text-align:center;">
                            <div style="
                                color:#6b7280;
                                font-size:13px;
                                text-transform:uppercase;
                                letter-spacing:1px;
                            ">
                                Broj karte
                            </div>

                            <div style="
                                font-size:26px;
                                font-weight:700;
                                color:#818cf8;
                                margin-top:5px;
                            ">
                                %s
                            </div>
                        </div>

                        <hr style="
                            border:none;
                            border-top:1px solid #e5e7eb;
                            margin:25px 0;
                        ">

                        <table style="
                            width:100%%;
                            border-collapse:collapse;
                            font-size:15px;
                        ">

                            <tr>
                                <td style="padding:10px 0;color:#666;">
                                    Događaj
                                </td>
                                <td style="text-align:right;font-weight:600;">
                                    %s
                                </td>
                            </tr>

                            <tr>
                                <td style="padding:10px 0;color:#666;">
                                    Datum
                                </td>
                                <td style="text-align:right;">
                                    %s - %s
                                </td>
                            </tr>

                            <tr>
                                <td style="padding:10px 0;color:#666;">
                                    Lokacija
                                </td>
                                <td style="text-align:right;">
                                    %s
                                </td>
                            </tr>

                            <tr>
                                <td style="padding:10px 0;color:#666;">
                                    Tip karte
                                </td>
                                <td style="text-align:right;">
                                    %s (%s)
                                </td>
                            </tr>

                            <tr>
                                <td style="padding:10px 0;color:#666;">
                                    Cena
                                </td>
                                <td style="
                                    text-align:right;
                                    font-size:18px;
                                    font-weight:700;
                                    color:#4ade80;
                                ">
                                    %s RSD
                                </td>
                            </tr>

                        </table>

                    </div>

                    <div style="
                        margin-top:30px;
                        padding:16px;
                        border-radius:10px;
                        background:#eef2ff;
                        color:#374151;
                        font-size:14px;
                    ">
                        Molimo Vas da sačuvate ovaj e-mail jer sadrži podatke
                        potrebne za identifikaciju Vaše karte.
                    </div>

                </div>

                <!-- Footer -->
                <div style="
                    text-align:center;
                    padding:20px;
                    background:#f8fafc;
                    color:#94a3b8;
                    font-size:12px;
                ">
                    Hvala što koristite EventSys.<br>
                    Ovo je automatski generisana poruka, molimo Vas da ne odgovarate na nju.
                </div>

            </div>

        </div>
        """.formatted(
                p.ime(),
                p.prezime(),
                p.nazivDogadjaja(),
                p.brojKarte(),
                p.nazivDogadjaja(),
                p.datumPocetka(),
                p.datumZavrsetka(),
                p.lokacija(),
                p.nazivTipa(),
                p.vrstaKarte(),
                p.cena()
        );
    }
}
