package eu.cqse;

import java.nio.file.Path;
import java.util.regex.Pattern;

public class SpecificFixesApplierForDeclaration extends FixerBase {

	protected SpecificFixesApplierForDeclaration(Path folder) {
		super(folder, "js");
	}

	public void fix() {
		adjustInAll("@record", "@interface");
		adjustInAll("{?Element}", "{Element|null}");
		adjustInAll(Pattern.compile("(?<=[{|(] ?)(Element|EventTarget)(?= ?[|})=])(?!\\|null)"), "?$1");
		adjustInAll(Pattern.compile("@(type|param|return)\\s+\\{([A-Z]\\w+)}"), "@$1 {?$2}");
		adjustInAll("IArrayLike", "ArrayLike");
		adjustIn("debug/tracer", "Trace_.TracerCallbacks", "TracerCallbacks");
		adjustIn("ui/palette", "Palette.CurrentCell_", "CurrentCell_");
		adjustIn("ui/palette", "CurrentCell_ = class ", "class CurrentCell_ ");
		adjustIn("i18n/datetimesymbols", "@struct", "");

		adjustIn("events/keycodes", "@enum {number}", "");
		adjustIn("events/keycodes", "let KeyCodes = ", "class KeyCodes ");
		adjustIn("events/keycodes", Pattern.compile("([A-Z_0-9]+):\\s*(\\d+),?"), "$1 = $2;");

		adjustIn("positioning/positioning", "@enum {number}", "");
		adjustIn("positioning/positioning", "let Corner =", "class Corner ");
		adjustIn("positioning/positioning", "let CornerBit =", "class CornerBit ");
		adjustIn("positioning/positioning", "let OverflowStatus =", "class OverflowStatus ");
		adjustIn("positioning/positioning", "let Overflow =", "class Overflow ");
		adjustIn("positioning/positioning", "OverflowStatus}", "number}");
		adjustIn("positioning/positioning", Pattern.compile("(?m) {2}([A-Z_]+):\\s*([^,}]*),?"), "  $1 = 0;");

		adjustIn("ui/dialog", Pattern.compile("([A-Z]+): Dialog\\.(?:[^,}]*)"), "$1: ''");
		adjustIn("ui/dialog", Pattern.compile("([A-Z]+): \\{\r?\n"), "$1 = {\n");
		adjustIn("ui/dialog", Pattern.compile("(key|caption): DefaultButton[^,}]*"), "$1: ''");

		adjustIn("net/errorcode", "@enum {number}", "");
		adjustIn("net/httpstatus", "@enum {number}", "");
	}
}
