package servicenow.common.soap;

import java.util.ArrayList;

import servicenow.common.soap.InsertResponse;
import servicenow.common.soap.Table;

/**
 * Holds the return value from 
 * {@link Table#insertMultiple(java.util.List) Table#insertMultiple}.
 */
@SuppressWarnings("serial")
public class InsertMultipleResponse extends ArrayList<InsertResponse> {

}
