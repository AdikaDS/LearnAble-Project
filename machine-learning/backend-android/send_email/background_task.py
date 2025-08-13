from fastapi import BackgroundTasks

def _enqueue_email(background_task: BackgroundTasks, fn, *args):
    try:
        background_task.add_task(fn, *args)
        return True
    except Exception as e:
        # log sesuai kebutuhan
        return False
