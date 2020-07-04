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

		adjustIn("google", "isArray(val: unknown): boolean", "isArray(val: unknown): val is unknown[]");
		adjustIn("google", "isBoolean(val: unknown): boolean", "isBoolean(val: unknown): val is boolean");
		adjustIn("google", "isDef(val: unknown): boolean", "isDef<T>(val: T): val is Exclude<typeof val, undefined>");
		adjustIn("google", "isDefAndNotNull(val: unknown): boolean", "isDefAndNotNull<T>(val: T): val is Exclude<typeof val, (undefined | null)>");
		adjustIn("google", "isNull(val: unknown): boolean", "isNull(val: unknown): val is null");
		adjustIn("google", "isNumber(val: unknown): boolean", "isNumber(val: unknown): val is number");
		adjustIn("google", "isObject(val: unknown): boolean", "isObject(val: unknown): val is object");
		adjustIn("google", "isString(val: unknown): boolean", "isString(val: unknown): val is string");

		adjustIn("asserts/asserts",
				"assertObject(value: any, opt_message?: string | undefined, ...args: any[]): any;",
				"assertObject<T>(value: T, opt_message?: string, ...args: any[]): T extends NonNullable<T> ? NonNullable<T> : never;");
		adjustIn("asserts/asserts", "fail(opt_message?: string | undefined, ...args: any[]): void;", "fail(opt_message?: string, ...args: any[]): never;");
		adjustIn("asserts/asserts", "assert<T>(condition: T, opt_message?: string | undefined, ...args: any[]): T;", "assert<T>(condition: T, opt_message?: string | undefined, ...args: any[]): asserts condition;");

		adjustIn("eventhandler", "export type ListenableType = EventTarget | Listenable | null;", "export type ListenableType = EventTarget | Listenable;");
		adjustIn("xhrio", "headers: StructsMap;", "headers: StructsMap<string, string>;");

		appendIn("xhrio", "import {Logger as DebugLogger} from \"../debug/logger\";\n\n");

		appendIn("combobox", "import {Logger as DebugLogger} from \"../debug/logger\";\n\n");
		adjustIn("events/eventtype", Pattern.compile("import MOUSE(.*)_1 = MOUSE([^;]+);"), "import MOUSE$1_1 = EventType.MOUSE$2;");
		adjustIn("events/eventtype", Pattern.compile("import MOUSE(.*)_2 = POINTER([^;]+);"), "import MOUSE$1_2 = PointerFallbackEventType.POINTER$2;");
		adjustIn("events/eventtype", Pattern.compile("import TOUCH(.*)_1 = POINTER([^;]+);"), "import TOUCH$1_1 = PointerTouchFallbackEventType.POINTER$2;");
		adjustIn("browserevent", Pattern.compile("import IE_BUTTON_MAP = IEButtonMap;\r?\n\\s+export \\{ IE_BUTTON_MAP };"), "export const IE_BUTTON_MAP: Array<number>;");

		adjustIn("events/eventhandler", "<EVENTOBJ>(", "<EVENTOBJ = BrowserEvent>(");
		adjustIn("events/eventhandler", "<T, EVENTOBJ>(", "<T = any, EVENTOBJ = BrowserEvent>(");
		appendIn("events/eventhandler", "\nimport { BrowserEvent } from './browserevent';");
		adjustIn("events/keycodes", Pattern.compile(" {4}([A-Z_]+)"), "    static $1");

		adjustIn("dom/dom", "NodeList<Element>", "NodeListOf<Element>");

		adjustIn("dom/dom", Pattern.compile("export ([^<]+)<T, R>\\((.*)string \\| TagName<T>(.*)\\): R( \\| null)?;"), "export $1($2string$3): Element$4;\nexport $1<T>($2TagName<T>$3): T$4;");
		adjustIn("dom/dom", Pattern.compile("export ([^<]+)<T, R>\\((.*)string \\| TagName<T>(.*)\\): (Array(?:Like)?)<R>( \\| null)?;"), "export $1($2string$3): $4<Element>$5;\nexport $1<T>($2TagName<T>$3): $4<T>$5;");

		adjustIn("positioning", Pattern.compile("(\\s{2,}[A-Z_]+): number;"), "$1,");
		adjustIn("positioning", Pattern.compile("export const ([A-Z_]+): number;"), "$1,");
		adjustIn("positioning","export class ", "export enum ");
		adjustIn("positioning","}\n" +
				"export namespace OverflowStatus {", "");

		prependIn("asserts/dom", "type Include<T, U> = T extends U ? T : never;\n");
		adjustIn("asserts/dom", Pattern.compile("Element\\(value: any\\): ([^;]+);"), "Element(value: any): Include<typeof value, $1>;");

		adjustIn("array/array", "export function concat(...args: any[]): Array<unknown>;", "export function concat<T>(...args: T[][]): Array<T>;\n" +
				"export function concat<T>(...args: T[]): Array<T>;\n" +
				"export function concat<T>(array: T[], element: T): Array<T>;\n" +
				"export function concat(...args: any[]): Array<unknown>;");
		adjustIn("array/array", "export function forEach<T, S>(arr: string | ArrayLike<T>, f: ((this: S, arg1: T, arg2: number, arg3: unknown) => unknown) | null, opt_obj?: S | undefined): void;",
				"export function forEach<T, S>(arr: string | ArrayLike<T>, f: ((this: S, arg1: T, arg2: number, arg3: unknown) => unknown) | null, opt_obj?: S | undefined): void;");

		adjustIn("classlist", "function get(element: Element | null): ArrayLike<unknown>;", "function get(element: Element | null): ArrayLike<string>;");

		adjustIn("object/object", "getValues<K, V>(obj: any): V[];", "getValues<T = unknown>(obj: Record<string, T> | ArrayLike<T> | object): T[];");
		adjustIn("ui/component", "forEachChild<T>(f: (this: T, arg1: unknown, arg2: number) => unknown, opt_obj?: T | undefined): void;", "forEachChild<T>(f: (this: T, arg1: Component, arg2: number) => unknown, opt_obj?: T | undefined): void;");
		adjustIn("object/object", "forEach<T, K, V>(obj: any, f: (this: T, arg1: V, arg2: unknown, arg3: any) => unknown, opt_obj?: T | undefined): void;", "forEach<T = unknown, K = unknown, V = unknown>(obj: Record<K, V> | object, f: (this: T, arg1: V, arg2: K, arg3: any) => any, opt_obj?: T | undefined): void;");
		adjustIn("events/eventhandler", "function unlisten<EVENTOBJ = BrowserEvent>(src: (EventTarget | Listenable) | null, type: string | string[] | EventId<EVENTOBJ> | EventId<EVENTOBJ>[], listener: (arg0: unknown) => unknown", "function unlisten<EVENTOBJ = BrowserEvent>(src: (EventTarget | Listenable) | null, type: string | string[] | EventId<EVENTOBJ> | EventId<EVENTOBJ>[], listener: (...arg0: any[]) => unknown");

		adjustIn("fx/dragdropgroup", "export class DragDropGroup extends AbstractDragDrop {", "export class DragDropGroup extends AbstractDragDrop {\n" +
				"    /**\n" +
				"     * Add item to drag object.\n" +
				"     *\n" +
				"     * @param {?Element|string} element Dom Node, or string representation of node\n" +
				"     *     id, to be used as drag source/drop target.\n" +
				"     * @param {Object=} opt_data Data associated with the source/target.\n" +
				"     * @throws Error If no element argument is provided or if the type is\n" +
				"     *     invalid\n" +
				"     * @override\n" +
				"     */\n" +
				"    addItem(element: string | Element | null, opt_data?: any): void;");
	}
}
