package org.epistem.diagram.model.server;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

/**
 * Handles mapping of URLs to workflows based on configuration given by a
 * diagram model
 *
 * @author nickmain
 */
public class WorkflowServlet extends HttpServlet {

    
    
    /** @see javax.servlet.GenericServlet#init(javax.servlet.ServletConfig) */
    @Override
    public void init( ServletConfig config ) throws ServletException {
        super.init( config );

    
    }

}
