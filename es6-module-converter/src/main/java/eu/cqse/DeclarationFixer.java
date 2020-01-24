package eu.cqse;

import java.nio.file.Path;
import java.util.regex.Pattern;

public class DeclarationFixer extends FixerBase {

	public DeclarationFixer(Path folder) {
		super(folder, "d.ts");
	}

	@Override
	protected void fix() {
		adjustInAll("(?<!\\w)(THIS|T|S|R|K|V|RESULT|VALUE|SCOPE|EVENTOBJ|TYPE|DEFAULT)_[123](?!\\w)", "$1");
		adjustInAll(" => ([);])", " => void$1");
		adjustInAll("opt_(\\w+): ", "opt_$1?: ");
		adjustIn("google", "isArray(val: any): boolean", "isArray(val: any): val is any[]");
		adjustIn("google", "isBoolean(val: any): boolean", "isBoolean(val: any): val is boolean");
		adjustIn("google", "isDef(val: any): boolean", "isDef<T>(val: T): val is Exclude<typeof val, undefined>");
		adjustIn("google", "isDefAndNotNull(val: any): boolean", "isDefAndNotNull<T>(val: T): val is Exclude<typeof val, (undefined | null)>");
		adjustIn("google", "isNull(val: any): boolean", "isNull(val: any): val is null");
		adjustIn("google", "isNumber(val: any): boolean", "isNumber(val: any): val is number");
		adjustIn("google", "isObject(val: any): boolean", "isObject(val: any): val is object");
		adjustIn("google", "isString(val: any): boolean", "isString(val: any): val is string");

		adjustIn("array", "forEach(arr: any, f: any,", "forEach<T>(arr: T[], f: (item : T, index : number) => void,");
		adjustIn("array", "map(arr: any, f: any,", "map<T>(arr: any, f: (item : T, index : number) => any,");
		adjustIn("asserts/asserts",
				"assertObject(value: any, opt_message?: string, ...args: any[]): any;",
				"assertObject<T>(value: T, opt_message?: string, ...args: any[]): T extends NonNullable<T> ? NonNullable<T> : never;");

		adjustIn("zippy", "role_: Role<string>;", " role_: Role;");
		adjustIn("tooltip", "elements_: StructsSet | null;", "elements_: StructsSet<any> | null;");
		adjustIn("popupmenu", "targets_: StructsMap;", "targets_: StructsMap<any, any>;");
		adjustIn("modalpopup", "(arg0: ...?)", "()");
		adjustIn("tagname", "export const ISINDEX: TagName<HTMLIsIndexElement>;", "");
		adjustIn("tagname", "export const MENUITEM: TagName<HTMLMenuItemElement>;", "");
		adjustIn("events/event", "target: Object | undefined;", "target: Object | null | undefined;");
		adjustIn("events/event", "currentTarget: Object | undefined;", "currentTarget: Object | null | undefined;");
		adjustIn("eventhandler", "keys_: Object<Key>;", "keys_: any;");
		adjustIn("xhrio", "headers: StructsMap;", "headers: StructsMap<string, string>;");
		adjustIn("control", "decorate(control: CONTROL, element: Element): Element;", "decorate(control: CONTROL, element: Element): Element | null;");

		appendIn("xhrio", "import {Logger as DebugLogger} from \"../debug/logger\";\n\n");
		adjustIn("xhrio", "logger_: LogLogger | null;", "logger_: DebugLogger | null;");

		appendIn("combobox", "import {Logger as DebugLogger} from \"../debug/logger\";\n\n");
		adjustIn("combobox", "logger_: LogLogger | null;", "logger_: DebugLogger | null;");
		adjustIn("autocomplete", "inputToAnchorMap_: Object<Element>;", "inputToAnchorMap_: any;");
		adjustIn("remotearraymatcher", "headers_: (Object | (StructsMap | null)) | null;", "headers_: (Object | (StructsMap<any, any> | null)) | null;");
		adjustIn("container", "openItem_: Control | null;", "openItem_: Control<any> | null;");
		adjustIn("control", "export const ariaAttributeMap_: Object<State, AriaState>;", "export const ariaAttributeMap_: any;");
		adjustIn("dialog", "set(key: any,", "// @ts-ignore\n    set(key: any,");
		adjustIn("labelinput", "eventHandler_: EventHandler | null;", "eventHandler_: EventHandler<any> | null;");
		adjustIn("paletterenderer", "decorate(palette: Control<any>, element: Element): null;", "decorate(palette: UiPalette, element: Element): null;");
		adjustIn("functions", "identity<T>(opt_returnValue?: T, var_args: any): T;", "identity<T>(opt_returnValue?: T, ...var_args: any): T;");
		adjustIn("events/eventtype", Pattern.compile("import MOUSE(.*)_1 = MOUSE([^;]+);"), "import MOUSE$1_1 = EventType.MOUSE$2;");
		adjustIn("events/eventtype", Pattern.compile("import MOUSE(.*)_2 = POINTER([^;]+);"), "import MOUSE$1_2 = PointerFallbackEventType.POINTER$2;");
		adjustIn("events/eventtype", Pattern.compile("import TOUCH(.*)_1 = POINTER([^;]+);"), "import TOUCH$1_1 = PointerTouchFallbackEventType.POINTER$2;");
		adjustIn("browserevent", Pattern.compile("import IE_BUTTON_MAP = IEButtonMap;\r?\n\\s+export \\{ IE_BUTTON_MAP };"), "export const IE_BUTTON_MAP: Array<number>;");
		adjustIn("safehtml", Pattern.compile("import from = htmlEscape;\r?\n\\s+export \\{ from };"), "export const htmlEscape: (textOrHtml: TextOrHtml_) => SafeHtml;");
		adjustIn("timer", Pattern.compile("export const defaultTimerObject: \\{\n\\s+setTimeout;\n\\s+clearTimeout;\n\\s+};"), "export const defaultTimerObject: any;");
		adjustIn("ac/renderer", Pattern.compile("customRenderer_: \\([^)]+[^;]+;"), "customRenderer_: any;");
	}
}
