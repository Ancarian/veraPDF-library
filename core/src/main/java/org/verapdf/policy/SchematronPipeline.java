/**
 * This file is part of veraPDF Library core, a module of the veraPDF project.
 * Copyright (c) 2015, veraPDF Consortium <info@verapdf.org> All rights
 * reserved. veraPDF Library core is free software: you can redistribute it
 * and/or modify it under the terms of either: The GNU General public license
 * GPLv3+. You should have received a copy of the GNU General Public License
 * along with veraPDF Library core as the LICENSE.GPL file in the root of the
 * source tree. If not, see http://www.gnu.org/licenses/ or
 * https://www.gnu.org/licenses/gpl-3.0.en.html. The Mozilla Public License
 * MPLv2+. You should have received a copy of the Mozilla Public License along
 * with veraPDF Library core as the LICENSE.MPL file in the root of the source
 * tree. If a copy of the MPL was not distributed with this file, you can obtain
 * one at http://mozilla.org/MPL/2.0/.
 */
/**
 * 
 */
package org.verapdf.policy;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.URIResolver;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

/**
 * @author <a href="mailto:carl@openpreservation.org">Carl Wilson</a>
 *         <a href="https://github.com/carlwilson">carlwilson AT github</a>
 * @version 0.1 Created 12 Dec 2016:16:52:10
 */

final class SchematronPipeline {
	static final ClassLoader cl = SchematronPipeline.class.getClassLoader();
	private static final TransformerFactory factory = getTransformerFactory();
	private static final String xslExt = ".xsl"; //$NON-NLS-1$
	private static final String resourcePath = "org/verapdf/policy/schematron/pipeline/"; //$NON-NLS-1$
	private static final String isoDsdlXsl = resourcePath + "iso_dsdl_include" + xslExt; //$NON-NLS-1$
	private static final String isoExpXsl = resourcePath + "iso_abstract_expand" + xslExt; //$NON-NLS-1$
	private static final String isoSvrlXsl = resourcePath + "iso_svrl_for_xslt1" + xslExt; //$NON-NLS-1$
	private static final Templates cachedIsoDsdXsl = createCachedTransform(isoDsdlXsl);
	private static final Templates cachedExpXsl = createCachedTransform(isoExpXsl);
	private static final Templates cachedIsoSvrlXsl = createCachedTransform(isoSvrlXsl);

	private SchematronPipeline() {
	}

	public static void processSchematron(InputStream schematronSource, OutputStream xslDest)
			throws TransformerException, IOException {
		File isoDsdResult = createTempFileResult(cachedIsoDsdXsl.newTransformer(), new StreamSource(schematronSource),
				"IsoDsd"); //$NON-NLS-1$
		File isoExpResult = createTempFileResult(cachedExpXsl.newTransformer(), new StreamSource(isoDsdResult),
				"ExpXsl"); //$NON-NLS-1$
		cachedIsoSvrlXsl.newTransformer().transform(new StreamSource(isoExpResult), new StreamResult(xslDest));
		isoDsdResult.delete();
		isoExpResult.delete();
	}

	static Templates createCachedTransform(final String transName) {
		try {
			return factory.newTemplates(new StreamSource(cl.getResourceAsStream(transName)));
		} catch (TransformerConfigurationException excep) {
			throw new IllegalStateException("Policy Schematron transformer XSL " + transName + " not found.", excep); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	private static File createTempFileResult(final Transformer transformer, final StreamSource toTransform,
			final String suffix) throws TransformerException, IOException {
		File result = File.createTempFile("veraPDF_", suffix); //$NON-NLS-1$
		result.deleteOnExit();

		try (FileOutputStream fos = new FileOutputStream(result)) {
			transformer.transform(toTransform, new StreamResult(fos));
		}
		return result;
	}

	private static TransformerFactory getTransformerFactory() {
		TransformerFactory fact = TransformerFactory.newInstance();
		fact.setURIResolver(new ClasspathResourceURIResolver());
		return fact;
	}

	private static class ClasspathResourceURIResolver implements URIResolver {
		ClasspathResourceURIResolver() {
			// Do nothing, just prevents synthetic access warning.
		}

		@Override
		public Source resolve(String href, String base) throws TransformerException {
			return new StreamSource(cl.getResourceAsStream(resourcePath + href));
		}
	}

}
