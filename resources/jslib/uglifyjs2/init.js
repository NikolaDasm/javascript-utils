var UglifyJS = exports;
var System  = Java.type("java.lang.System");

UglifyJS.AST_Node.warn_function = function(txt) {
    System.out.println(txt);
};

exports.minify = function(codes, options) {
    options = UglifyJS.defaults(options, {
        spidermonkey     : false,
        outSourceMap     : null,
        sourceRoot       : null,
        inSourceMap      : null,
        fromString       : false,
        warnings         : false,
        mangle           : {},
        mangleProperties : false,
        nameCache        : null,
        output           : null,
        compress         : {},
        parse            : {}
    });
    UglifyJS.base54.reset();
	
	if (typeof codes == "string") codes = [ codes ];
	
	// 1. parse
    var toplevel = null;

	codes.forEach(function(code){
		toplevel = UglifyJS.parse(code, {
			filename: "?",
			toplevel: toplevel
		});
	});
	
	// 2. compress
	if (options.compress) {
        var compress = { warnings: options.warnings };
        UglifyJS.merge(compress, options.compress);
        toplevel.figure_out_scope();
        var sq = UglifyJS.Compressor(compress);
        toplevel = toplevel.transform(sq);
    }
	
	// 3. mangle properties
	if (options.mangleProperties || options.nameCache) {
		options.mangleProperties.cache = UglifyJS.readNameCache(options.nameCache, "props");
		toplevel = UglifyJS.mangle_properties(toplevel, options.mangleProperties);
		UglifyJS.writeNameCache(options.nameCache, "props", options.mangleProperties.cache);
	}

	// 4. mangle
	if (options.mangle) {
		toplevel.figure_out_scope(options.mangle);
		toplevel.compute_char_frequency(options.mangle);
		toplevel.mangle_names(options.mangle);
	}

    // 5. output
	var stream = UglifyJS.OutputStream();
	toplevel.print(stream);
	return stream.toString();
};
