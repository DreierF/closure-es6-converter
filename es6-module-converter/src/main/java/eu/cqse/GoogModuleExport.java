package eu.cqse;

/**
 * Bundles some information about a (Google module specific) 'exports' statement.
 */
public class GoogModuleExport {

    /**
     * The name of the exported function/constant/...
     */
    public final String exportName;

    /**
     * <p>Whether the export happens inline, e.g.</p>
     * <code>
     * exports function foo() ...
     * </code>
     * <p>The alternative is an export list, like </p>
     * <code>
     * exports {foo, bar}
     * </code>
     * <p>
     */
    public final boolean isInlineExport;

    /**
     * The full match of this export, which can be used to replace it with other content later.
     * <ul>
     *     <li>In case of an inline export (see {@link #isInlineExport}) , this will also include the '=' sign.</li>
     *     <li>For an export list, this will be the entire 'exports {...};' statement (even if there are other exports) </li>
     * </ul>
     */
    public final String fullMatch;

    public GoogModuleExport(String exportName, boolean isInlineExport, String fullMatch) {
        this.exportName = exportName;
        this.isInlineExport = isInlineExport;
        this.fullMatch = fullMatch;
    }
}
