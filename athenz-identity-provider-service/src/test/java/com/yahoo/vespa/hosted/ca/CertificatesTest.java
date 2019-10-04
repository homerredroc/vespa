// Copyright 2019 Oath Inc. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.
package com.yahoo.vespa.hosted.ca;

import com.yahoo.security.KeyAlgorithm;
import com.yahoo.security.KeyUtils;
import com.yahoo.security.SubjectAlternativeName;
import com.yahoo.test.ManualClock;
import org.junit.Test;

import java.security.KeyPair;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.List;

import static java.time.temporal.ChronoUnit.SECONDS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author mpolden
 */
public class CertificatesTest {

    private final KeyPair keyPair = KeyUtils.generateKeypair(KeyAlgorithm.EC, 256);
    private final X509Certificate caCertificate = CertificateTester.createCertificate("CA", keyPair);

    @Test
    public void expiry() {
        var clock = new ManualClock();
        var certificates = new Certificates(clock);
        var csr = CertificateTester.createCsr();
        var certificate = certificates.create(csr, caCertificate, keyPair.getPrivate());
        var now = clock.instant();

        assertEquals(now.minus(Duration.ofHours(1)).truncatedTo(SECONDS), certificate.getNotBefore().toInstant());
        assertEquals(now.plus(Duration.ofDays(30)).truncatedTo(SECONDS), certificate.getNotAfter().toInstant());
    }

    @Test
    public void add_san_from_csr() throws Exception {
        var certificates = new Certificates(new ManualClock());
        var dnsName = "host.example.com";
        var csr = CertificateTester.createCsr(dnsName);
        var certificate = certificates.create(csr, caCertificate, keyPair.getPrivate());

        assertNotNull(certificate.getSubjectAlternativeNames());
        assertEquals(1, certificate.getSubjectAlternativeNames().size());
        assertEquals(List.of(SubjectAlternativeName.Type.DNS_NAME.getTag(), dnsName),
                     certificate.getSubjectAlternativeNames().iterator().next());
    }

}
