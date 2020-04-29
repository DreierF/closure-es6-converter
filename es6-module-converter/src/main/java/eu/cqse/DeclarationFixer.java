package eu.cqse;

import java.nio.file.Path;
import java.util.regex.Pattern;

public class DeclarationFixer extends FixerBase {

	public DeclarationFixer(Path folder) {
		super(folder, "d.ts");
	}

	@Override
	protected void fix() {
		adjustInAll(Pattern.compile("(?<!\\w)(THIS|T|S|R|K|V|RESULT|VALUE|SCOPE|EVENTOBJ|TYPE|DEFAULT)_[123](?!\\w)"), "$1");
		adjustInAll(Pattern.compile(" => ([);])"), " => void$1");
		adjustInAll(Pattern.compile("opt_(\\w+): "), "opt_$1?: ");
		adjustInAll(", var_args: any", ", ...var_args: any");

		adjustIn("google", "isArray(val: any): boolean", "isArray(val: any): val is any[]");
		adjustIn("google", "isBoolean(val: any): boolean", "isBoolean(val: any): val is boolean");
		adjustIn("google", "isDef(val: any): boolean", "isDef<T>(val: T): val is Exclude<typeof val, undefined>");
		adjustIn("google", "isDefAndNotNull(val: any): boolean", "isDefAndNotNull<T>(val: T): val is Exclude<typeof val, (undefined | null)>");
		adjustIn("google", "isNull(val: any): boolean", "isNull(val: any): val is null");
		adjustIn("google", "isNumber(val: any): boolean", "isNumber(val: any): val is number");
		adjustIn("google", "isObject(val: any): boolean", "isObject(val: any): val is object");
		adjustIn("google", "isString(val: any): boolean", "isString(val: any): val is string");

		adjustIn("asserts/asserts",
				"assertObject(value: any, opt_message?: string | undefined, ...args: any[]): any;",
				"assertObject<T>(value: T, opt_message?: string, ...args: any[]): T extends NonNullable<T> ? NonNullable<T> : never;");
		adjustIn("asserts/asserts", "fail(opt_message?: string | undefined, ...args: any[]): void;", "fail(opt_message?: string, ...args: any[]): never;");
		adjustIn("asserts/asserts", "assert<T>(condition: T, opt_message?: string | undefined, ...args: any[]): T;", "assert<T>(condition: T, opt_message?: string | undefined, ...args: any[]): asserts condition;");

		adjustIn("zippy", "role_: Role<string>;", " role_: Role;");
		adjustIn("tooltip", "elements_: StructsSet | null;", "elements_: StructsSet<any> | null;");
		adjustIn("popupmenu", "targets_: StructsMap;", "targets_: StructsMap<any, any>;");
		adjustIn("modalpopup", "(arg0: ...?)", "()");
		adjustIn("tagname", "export const ISINDEX: TagName<HTMLIsIndexElement>;", "");
		adjustIn("tagname", "export const MENUITEM: TagName<HTMLMenuItemElement>;", "");
		adjustIn("events/event", "target: Object | undefined;", "target: Object | null | undefined;");
		adjustIn("events/event", "currentTarget: Object | undefined;", "currentTarget: Object | null | undefined;");
		adjustIn("eventhandler", "keys_: Object<Key>;", "keys_: any;");
		adjustIn("eventhandler", "export type ListenableType = EventTarget | Listenable | null;", "export type ListenableType = EventTarget | Listenable;");
		adjustIn("xhrio", "headers: StructsMap;", "headers: StructsMap<string, string>;");

		appendIn("xhrio", "import {Logger as DebugLogger} from \"../debug/logger\";\n\n");
		adjustIn("xhrio", "logger_: LogLogger | null;", "logger_: DebugLogger | null;");

		appendIn("combobox", "import {Logger as DebugLogger} from \"../debug/logger\";\n\n");
		adjustIn("combobox", "logger_: LogLogger | null;", "logger_: DebugLogger | null;");
		adjustIn("autocomplete", "inputToAnchorMap_: Object<Element>;", "inputToAnchorMap_: any;");
		adjustIn("remotearraymatcher", "headers_: (Object | (StructsMap | null)) | null;", "headers_: (Object | (StructsMap<any, any> | null)) | null;");
		adjustIn("container", "openItem_: Control | null;", "openItem_: Control<any> | null;");
//		adjustIn("control", "export const ariaAttributeMap_: Object<State, AriaState>;", "export const ariaAttributeMap_: any;");
		adjustIn("dialog", "set(key: any,", "// @ts-ignore\n    set(key: any,");
		adjustIn("labelinput", "eventHandler_: EventHandler | null;", "eventHandler_: EventHandler<any> | null;");
		adjustIn("paletterenderer", "decorate(palette: Control<any> | null, element: Element | null): null;", "decorate(palette: UiPalette | null, element: Element | null): null;");
		adjustIn("events/eventtype", Pattern.compile("import MOUSE(.*)_1 = MOUSE([^;]+);"), "import MOUSE$1_1 = EventType.MOUSE$2;");
		adjustIn("events/eventtype", Pattern.compile("import MOUSE(.*)_2 = POINTER([^;]+);"), "import MOUSE$1_2 = PointerFallbackEventType.POINTER$2;");
		adjustIn("events/eventtype", Pattern.compile("import TOUCH(.*)_1 = POINTER([^;]+);"), "import TOUCH$1_1 = PointerTouchFallbackEventType.POINTER$2;");
		adjustIn("browserevent", Pattern.compile("import IE_BUTTON_MAP = IEButtonMap;\r?\n\\s+export \\{ IE_BUTTON_MAP };"), "export const IE_BUTTON_MAP: Array<number>;");
//		adjustIn("safehtml", Pattern.compile("import from = htmlEscape;\r?\n\\s+export \\{ from };"), "export const htmlEscape: (textOrHtml: TextOrHtml_) => SafeHtml;");
//		adjustIn("timer", Pattern.compile("export const defaultTimerObject: \\{\n\\s+setTimeout;\n\\s+clearTimeout;\n\\s+};"), "export const defaultTimerObject: any;");
		adjustIn("ac/renderer", Pattern.compile("customRenderer_: \\([^)]+[^;]+;"), "customRenderer_: any;");

		adjustIn("events/eventhandler", "listener: (this: T, arg1: EVENTOBJ) => any", "listener: (this: T, arg1: EVENTOBJ) => boolean|void");
		adjustIn("events/eventhandler", "<EVENTOBJ>(", "<EVENTOBJ = BrowserEvent>(");
		adjustIn("events/eventhandler", "<T, EVENTOBJ>(", "<T = any, EVENTOBJ = BrowserEvent>(");
		appendIn("events/eventhandler", "\nimport { BrowserEvent } from './browserevent';");
		adjustIn("events/keycodes", Pattern.compile(" {4}([A-Z_]+)"), "    static $1");

		adjustIn("dom/dom", "getElementsByTagName<T, R>(tagName: TagName<T>, opt_parent?: Element | Document | undefined): any;", "getElementsByTagName<T>(tagName: TagName<T>, opt_parent?: Element | Document): HTMLCollectionOf<T>;");
		adjustIn("dom/dom", "getChildren(element: Element | null): any;", "getChildren(element: Element | null): ArrayLike<Element>;");

		adjustIn("dom/dom", Pattern.compile("export ([^<]+)<T, R>\\((.*)string \\| TagName<T>(.*)\\): R( \\| null)?;"), "export $1($2string$3): Element$4;\nexport $1<T>($2TagName<T>$3): T$4;");
		adjustIn("dom/dom", Pattern.compile("export ([^<]+)<T, R>\\((.*)string \\| TagName<T>(.*)\\): (Array(?:Like)?)<R>( \\| null)?;"), "export $1($2string$3): $4<Element>$5;\nexport $1<T>($2TagName<T>$3): $4<T>$5;");

		prependIn("asserts/dom", "type Include<T, U> = T extends U ? T : never;\n");
		adjustIn("asserts/dom", Pattern.compile("Element\\(value: any\\): ([^;]+);"), "Element(value: any): Include<typeof value, $1>;");

		adjustIn("object/object", "getValues<K, V>(obj: any): V[];", "getValues<T = unknown>(obj: Record<string, T> | ArrayLike<T> | object): T[];");
	}
}
