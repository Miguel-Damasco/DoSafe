package com.miguel_damasco.DoSafe.document.domain.extraction;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.miguel_damasco.DoSafe.document.domain.DocumentTypeEnum;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class PassportDateExtractor implements ExpirationDateExtractor {

    @Override
    public boolean supports(DocumentTypeEnum type) {
        return type == DocumentTypeEnum.PASSPORT;
    }

    @Override
    public LocalDate extract(List<String> pLines, UUID pDocumentId, long pUserId) {

		log.info("Starting passport date extraction documentId={} userId={}", pDocumentId, pUserId);

        LocalDate localDate = null;

        for(int i = 0; i < pLines.size(); i++) {

			// Expects Uruguayan passport OCR format:
			// Line i:   "VENCIMENTO"
			// Line i+1: (ignored - label line)
			// Line i+2: "DD MMM YYYY" e.g. "15 Ene 2030"

        	if(i < pLines.size() - 2  && (pLines.get(i).contains("VENCIMENTO"))) {

        		StringBuilder sb = new StringBuilder();

        		String date = pLines.get(i + 2);

        		char[] dateArray = new char[8];

        		dateArray[0] = date.charAt(0);
        		dateArray[1] = date.charAt(1);

                log.debug("Day extracted documentId={} day={}{}", pDocumentId, dateArray[0], dateArray[1]);

        		for(int j = 3; j < 6; j++) {

        			sb.append(date.charAt(j));
        		}

        		byte[] monthSelector = switch(sb.toString()) {

	        		case "Ene" -> new byte[] {0,1};
	        		case "Feb" -> new byte[] {0,2};
	        		case "Mar" -> new byte[] {0,3};
	        		case "Abr" -> new byte[] {0,4};
	        		case "May" -> new byte[] {0,5};
	        		case "Jun" -> new byte[] {0,6};
	        		case "Jul" -> new byte[] {0,7};
	        		case "Ago" -> new byte[] {0,8};
	        		case "Sep" -> new byte[] {0,9};
	        		case "Oct" -> new byte[] {1,0};
	        		case "Nov" -> new byte[] {1,1};
	        		case "Dic" -> new byte[] {1,2};
	        		default -> new byte[] {0,0};
        		};

                // Unrecognized month is a legitimate "cannot extract" case — return null
                // so the user can enter the date manually.
        		if(monthSelector[0] == 0 && monthSelector[1] == 0) {
                    log.warn("Unrecognized month abbreviation documentId={} raw={}", pDocumentId, sb);
                    return null;
                }

        		dateArray[2] = (char) (monthSelector[0] + 48);
        		dateArray[3] = (char) (monthSelector[1] + 48);

                log.debug("Month extracted documentId={} month={}{}", pDocumentId, dateArray[2], dateArray[3]);

        		byte count = 7;

        		for(int k = date.length() - 1; count >= 4; k--) {

        			dateArray[count--] = date.charAt(k);
        		}

        		String year = "" + dateArray[4] + dateArray[5] + dateArray[6] + dateArray[7];

                log.debug("Year extracted documentId={} year={}", pDocumentId, year);

        		String finalDate = String.format("%s%s/%s%s/%s", dateArray[0], dateArray[1], dateArray[2], dateArray[3], year);

        		DateTimeFormatter formater = DateTimeFormatter.ofPattern("dd/MM/yyy");

        		localDate = LocalDate.parse(finalDate, formater);

        	}
        }

        if(localDate == null) log.warn("Expiration date not found documentId={} userId={}", pDocumentId, pUserId);
        else log.info("Expiration date extracted documentId={} userId={} date={}", pDocumentId, pUserId, localDate);

        return localDate;
    }

}
