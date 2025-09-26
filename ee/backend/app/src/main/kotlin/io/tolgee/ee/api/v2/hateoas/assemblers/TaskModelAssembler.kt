package io.tolgee.ee.api.v2.hateoas.assemblers

import io.tolgee.dtos.cacheable.LanguageDto
import io.tolgee.ee.api.v2.controllers.TaskController
import io.tolgee.hateoas.TranslationAgencySimpleModelAssembler
import io.tolgee.hateoas.language.LanguageModelAssembler
import io.tolgee.hateoas.task.TaskModel
import io.tolgee.hateoas.userAccount.SimpleUserAccountModelAssembler
import io.tolgee.model.views.TaskWithScopeView
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport
import org.springframework.stereotype.Component

@Component
class TaskModelAssembler(
  private val simpleUserAccountModelAssembler: SimpleUserAccountModelAssembler,
  private val languageModelAssembler: LanguageModelAssembler,
  private val translationAgencySimpleModelAssembler: TranslationAgencySimpleModelAssembler,
) : RepresentationModelAssemblerSupport<TaskWithScopeView, TaskModel>(
    TaskController::class.java,
    TaskModel::class.java,
  ) {
  override fun toModel(entity: TaskWithScopeView): TaskModel {
    return TaskModel(
      number = entity.number,
      name = entity.name,
      description = entity.description,
      type = entity.type,
      language =
        entity.language.let {
          languageModelAssembler.toModel(
            LanguageDto.fromEntity(
              it,
              entity.project.baseLanguage?.id,
            ),
          )
        },
      dueDate = entity.dueDate?.time,
      assignees = entity.assignees.map { simpleUserAccountModelAssembler.toModel(it) }.toMutableSet(),
      author = entity.author?.let { simpleUserAccountModelAssembler.toModel(it) },
      createdAt = entity.createdAt?.time,
      closedAt = entity.closedAt?.time,
      totalItems = entity.totalItems,
      doneItems = entity.doneItems,
      baseWordCount = entity.baseWordCount,
      baseCharacterCount = entity.baseCharacterCount,
      state = entity.state,
      agency = entity.agency?.let { translationAgencySimpleModelAssembler.toModel(it) },
    )
  }
}
