def get_context_param(req, context_name_suffix, param_key):
    contexts = req.get("queryResult", {}).get("outputContexts", [])
    for context in contexts:
        if context_name_suffix in context.get("name", ""):
            return context.get("parameters", {}).get(param_key)
    return None
